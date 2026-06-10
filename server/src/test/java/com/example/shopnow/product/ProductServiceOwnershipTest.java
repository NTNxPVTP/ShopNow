package com.example.shopnow.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.product.models.Product;
import com.example.shopnow.product.models.ProductStatus;
import com.example.shopnow.product.models.Shop;
import com.example.shopnow.product.rest.dto.CreateProductRequest;
import com.example.shopnow.product.rest.dto.ProductDetailResponse;
import com.example.shopnow.user.api.AuthenticatedUser;

/**
 * Unit test cho kiểm tra quyền sở hữu shop trên {@link AuthenticatedUser} trong
 * {@link ProductServiceImpl}.
 *
 * <p><b>Validates: Requirements 5.4</b>
 *
 * <p>Sau khi refactor module, {@link ProductServiceImpl} nhận principal công khai
 * {@link AuthenticatedUser} (thay vì entity {@code User} nội bộ) để kiểm tra quyền
 * sở hữu. Test này xác nhận ngữ nghĩa cũ được giữ nguyên:
 * <ul>
 *   <li>Principal có {@code getId()} khớp {@code shop.ownerId} (SELLER hợp lệ) thì
 *       thao tác thành công.</li>
 *   <li>Principal không đủ quyền (id khác chủ shop, hoặc id null) bị từ chối với
 *       {@link ErrorCode#PRODUCT_ACCESS_DENIED} / {@link ErrorCode#USER_NOT_FOUND}.</li>
 * </ul>
 *
 * <p>{@link AuthenticatedUser} được mock qua Mockito; chỉ stub {@code getId()} vì đó
 * là dữ liệu duy nhất mà logic kiểm tra quyền sở hữu sử dụng.
 */
class ProductServiceOwnershipTest {

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

    @Test
    @DisplayName("createProduct: principal sở hữu shop (id khớp ownerId) thao tác thành công")
    void createProduct_ownerIdMatchesShopOwner_succeeds() {
        // --- Arrange ---
        UUID ownerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();

        AuthenticatedUser owner = Mockito.mock(AuthenticatedUser.class);
        when(owner.getId()).thenReturn(ownerId);

        // categoryIds null => resolveCategories trả về Set.of(), bỏ qua category.
        CreateProductRequest request = new CreateProductRequest(
                "Test Product",
                "http://example.com/pic.jpg",
                10,
                BigDecimal.valueOf(99.99),
                shopId,
                null);

        Shop shop = new Shop();
        ReflectionTestUtils.setField(shop, "id", shopId);
        shop.setOwnerId(ownerId); // ownerId khớp principal => quyền hợp lệ
        shop.setName("Test Shop");
        shop.setIsActive(true);

        Product product = Product.builder()
                .name("Test Product")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .isDeleted(false)
                .build();
        UUID productId = UUID.randomUUID();
        ReflectionTestUtils.setField(product, "id", productId);

        ProductDetailResponse expected = new ProductDetailResponse(
                productId, "Test Product", "http://example.com/pic.jpg",
                10, BigDecimal.valueOf(99.99), ProductStatus.ACTIVE, shopId, Set.of());

        when(productMapper.fromCreateRequestToProduct(any(CreateProductRequest.class)))
                .thenReturn(product);
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toDto(any(Product.class))).thenReturn(expected);

        // --- Act ---
        ProductDetailResponse actual = productService.createProduct(request, owner);

        // --- Assert ---
        assertThat(actual).isEqualTo(expected);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("createProduct: principal không sở hữu shop bị từ chối PRODUCT_ACCESS_DENIED")
    void createProduct_ownerIdDoesNotMatchShopOwner_throwsAccessDenied() {
        // --- Arrange ---
        UUID ownerId = UUID.randomUUID();
        UUID otherOwnerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();

        AuthenticatedUser owner = Mockito.mock(AuthenticatedUser.class);
        when(owner.getId()).thenReturn(ownerId);

        CreateProductRequest request = new CreateProductRequest(
                "Test Product",
                "http://example.com/pic.jpg",
                10,
                BigDecimal.valueOf(99.99),
                shopId,
                null);

        Shop shop = new Shop();
        ReflectionTestUtils.setField(shop, "id", shopId);
        shop.setOwnerId(otherOwnerId); // chủ shop là người khác => quyền không hợp lệ
        shop.setName("Test Shop");
        shop.setIsActive(true);

        Product product = Product.builder()
                .name("Test Product")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .isDeleted(false)
                .build();

        lenient().when(productMapper.fromCreateRequestToProduct(any(CreateProductRequest.class)))
                .thenReturn(product);
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));

        // --- Act & Assert ---
        assertThatThrownBy(() -> productService.createProduct(request, owner))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_ACCESS_DENIED);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("deleteProduct: principal có id null bị từ chối USER_NOT_FOUND")
    void deleteProduct_ownerIdNull_throwsUserNotFound() {
        // --- Arrange ---
        UUID productId = UUID.randomUUID();

        AuthenticatedUser owner = Mockito.mock(AuthenticatedUser.class);
        when(owner.getId()).thenReturn(null);

        // --- Act & Assert ---
        assertThatThrownBy(() -> productService.deleteProduct(productId, owner))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(productRepository, never()).softDeleteProductByIdAndShopOwnerId(any(UUID.class), any(UUID.class));
    }

    @Test
    @DisplayName("deleteProduct: principal hợp lệ nhưng không sở hữu sản phẩm bị từ chối PRODUCT_NOT_FOUND")
    void deleteProduct_notOwnedByPrincipal_throwsProductNotFound() {
        // --- Arrange ---
        UUID ownerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        AuthenticatedUser owner = Mockito.mock(AuthenticatedUser.class);
        when(owner.getId()).thenReturn(ownerId);

        // softDelete trả về 0 dòng bị ảnh hưởng => không sở hữu sản phẩm.
        when(productRepository.softDeleteProductByIdAndShopOwnerId(productId, ownerId)).thenReturn(0);

        // --- Act & Assert ---
        assertThatThrownBy(() -> productService.deleteProduct(productId, owner))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }
}
