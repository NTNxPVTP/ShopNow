package com.example.shopnow.product;

import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.product.models.Product;
import com.example.shopnow.product.models.ProductStatus;
import com.example.shopnow.product.models.Shop;
import com.example.shopnow.product.rest.dto.CreateProductRequest;
import com.example.shopnow.product.rest.dto.ProductDetailResponse;
import com.example.shopnow.user.models.Role;
import com.example.shopnow.user.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Exploration test for BUG-24 (non-SELLER user can create product).
 *
 * <p><b>Validates: Requirements 2.24</b> (Property 24 in design.md).
 *
 * <p>Phase 1 bug-condition exploration test for the bugfix workflow
 * {@code shopnow-codebase-bugfixes}. It is EXPECTED TO FAIL on unfixed code;
 * the failure surfaces a counterexample that proves the bug exists. After
 * the fix in {@link ProductService#createProduct}, this test must turn green.
 *
 * <p><b>Bug condition C(24):</b> A {@link User} with
 * {@code role ∈ {CUSTOMER, ADMIN}} (i.e. non-SELLER) owns a {@link Shop}
 * ({@code shop.ownerId == user.id}) and calls
 * {@link ProductService#createProduct(CreateProductRequest, User)}.
 *
 * <p><b>Property P(24):</b> for every input satisfying C(24),
 * {@link ProductService#createProduct} SHALL reject the request with
 * {@link DomainException} carrying {@link ErrorCode#PRODUCT_ACCESS_DENIED}
 * (or similar access-denied error). No product SHALL be persisted.
 *
 * <p><b>Buggy production behaviour:</b>
 * <pre>
 *   public ProductDetailResponse createProduct(CreateProductRequest request, User owner) {
 *       ...
 *       UUID shopOwnerId = owner.getId();
 *       if (shopOwnerId == null || !shopOwnerId.equals(shop.getOwnerId())) {
 *           throw new DomainException(ErrorCode.PRODUCT_ACCESS_DENIED);
 *       }
 *       // ← NO role check here! Any user whose id matches shop.ownerId passes.
 *       ...
 *   }
 * </pre>
 * Because the code only checks {@code shopOwnerId.equals(shop.getOwnerId())}
 * without verifying {@code owner.getRole() == Role.SELLER}, a CUSTOMER or
 * ADMIN who happens to own a shop (e.g. via direct DB manipulation or an
 * incomplete shop-creation flow) can successfully create products.
 *
 * <p><b>Test design:</b> Mockito-style unit test that wires the real
 * {@link ProductService} with mocked dependencies. For each non-SELLER role,
 * we build a User with that role, set up a Shop whose ownerId matches the
 * user's id, and call createProduct. The test asserts that a
 * {@link DomainException} with an access-denied error code is thrown.
 * On unfixed code, the call succeeds (product is created) because only
 * ownerId is checked, not role.
 *
 * <p><b>Expected counterexample on unfixed code:</b> User with
 * role=CUSTOMER, id=X, calling createProduct for a shop with ownerId=X
 * → product is created successfully instead of being rejected.
 */
class ProductServiceBug24ExplorationTest {

    private ProductRepository productRepository;
    private ShopRepository shopRepository;
    private CategoryRepository categoryRepository;
    private ProductMapper productMapper;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productRepository = Mockito.mock(ProductRepository.class);
        shopRepository = Mockito.mock(ShopRepository.class);
        categoryRepository = Mockito.mock(CategoryRepository.class);
        productMapper = Mockito.mock(ProductMapper.class);

        productService = new ProductServiceImpl(
                productRepository, shopRepository, categoryRepository, productMapper);
    }

    /**
     * For each non-SELLER role (CUSTOMER, ADMIN), a user who owns a shop
     * must NOT be allowed to create a product. On unfixed code, the ownerId
     * check passes and the product is created — test FAILS.
     */
    @ParameterizedTest(name = "role={0} → createProduct SHALL throw PRODUCT_ACCESS_DENIED")
    @EnumSource(value = Role.class, names = {"CUSTOMER", "ADMIN"})
    @DisplayName("BUG-24: non-SELLER user who owns a shop SHALL NOT create product")
    void createProduct_nonSellerOwner_shallThrowAccessDenied(Role nonSellerRole) {

        // --- Arrange --------------------------------------------------------
        UUID userId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();

        // Build a User with a non-SELLER role
        User owner = User.builder()
                .name("TestUser")
                .email("test@example.com")
                .password("hashed")
                .role(nonSellerRole)
                .build();
        ReflectionTestUtils.setField(owner, "id", userId);

        // Build a Shop whose ownerId matches the user's id
        Shop shop = new Shop();
        ReflectionTestUtils.setField(shop, "id", shopId);
        shop.setOwnerId(userId); // ownerId == user.id → passes the existing check
        shop.setName("TestShop");
        shop.setIsActive(true);

        // Build a valid CreateProductRequest
        CreateProductRequest request = new CreateProductRequest(
                "Test Product",
                "http://example.com/pic.jpg",
                10,
                BigDecimal.valueOf(99.99),
                shopId,
                Set.of());

        // Stub shopRepository to return the shop
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));

        // Stub mapper to return a Product from the request (needed if code
        // reaches past the guard)
        Product product = Product.builder()
                .name("Test Product")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .isDeleted(false)
                .shop(shop)
                .build();
        ReflectionTestUtils.setField(product, "id", UUID.randomUUID());

        when(productMapper.fromCreateRequestToProduct(any(CreateProductRequest.class)))
                .thenReturn(product);
        lenient().when(productRepository.save(any(Product.class))).thenReturn(product);
        lenient().when(productMapper.toDto(any(Product.class)))
                .thenReturn(new ProductDetailResponse(
                        product.getId(), "Test Product", "http://example.com/pic.jpg",
                        10, BigDecimal.valueOf(99.99), ProductStatus.ACTIVE,
                        shopId, Set.of()));

        // --- Act ------------------------------------------------------------
        Throwable thrown = catchThrowable(() -> productService.createProduct(request, owner));

        // --- Assert ---------------------------------------------------------
        // P(24): non-SELLER must be rejected with an access-denied error.
        // On unfixed code, thrown == null (product created successfully)
        // because only ownerId is checked, not role.
        assertThat(thrown)
                .as("createProduct called by user with role=%s (id=%s) who owns shop (ownerId=%s) "
                                + "SHALL throw DomainException with access-denied error code; "
                                + "on unfixed code, no exception is thrown (product is created)",
                        nonSellerRole, userId, userId)
                .isNotNull()
                .isInstanceOf(DomainException.class);

        DomainException domainEx = (DomainException) thrown;
        assertThat(domainEx.getErrorCode())
                .as("Error code should indicate access denied for non-SELLER role=%s", nonSellerRole)
                .isEqualTo(ErrorCode.PRODUCT_ACCESS_DENIED);
    }
}
