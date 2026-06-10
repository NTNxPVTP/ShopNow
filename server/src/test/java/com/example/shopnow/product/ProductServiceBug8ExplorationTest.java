package com.example.shopnow.product;

import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.order.rest.dto.OrderItemRequest;
import com.example.shopnow.product.api.dto.OrderLineRequest;
import com.example.shopnow.product.api.dto.ProductInfoForOrder;
import com.example.shopnow.product.models.Product;
import com.example.shopnow.product.models.ProductStatus;
import com.example.shopnow.product.models.Shop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Exploration test for BUG-8 (duplicate productId in {@code CreateOrderRequest}
 * triggers spurious PRODUCT_NOT_FOUND).
 *
 * <p><b>Validates: Requirements 2.8</b> (Property 8 in design.md).
 *
 * <p>Phase 1 bug-condition exploration test for the bugfix workflow
 * {@code shopnow-codebase-bugfixes}. It is EXPECTED TO FAIL on unfixed code;
 * the failure surfaces a counterexample that proves the bug exists. After
 * the fix in {@link ProductService#decreaseProducts}, this test must turn
 * green.
 *
 * <p><b>Bug condition C(8):</b> {@code itemRequests} contains two or more
 * {@link OrderItemRequest} entries whose {@code productId} fields are equal,
 * and every referenced product is persisted, {@code ACTIVE}, non-deleted,
 * and has sufficient stock for the summed quantity.
 *
 * <p><b>Property P(8):</b> for every input satisfying C(8),
 * {@link ProductService#decreaseProducts(List)} SHALL NOT throw
 * {@link DomainException} with {@link ErrorCode#PRODUCT_NOT_FOUND}.
 * Per the policy chosen in design.md, the call is processed (with the
 * effective per-product quantity = sum of duplicate quantities), so the
 * downstream {@link com.example.shopnow.order.OrderService#createOrder
 * OrderService.createOrder} call also avoids that error code.
 *
 * <p><b>Buggy production behaviour:</b>
 * <pre>
 *   public List&lt;ProductInfoForOrder&gt; decreaseProducts(List&lt;OrderItemRequest&gt; itemRequests) {
 *       List&lt;UUID&gt; ids = itemRequests.stream().map(OrderItemRequest::productId).toList();
 *       List&lt;Product&gt; products = getProducts(ids);            // SQL IN (...) returns DISTINCT rows
 *       if (products.size() &lt; itemRequests.size()) {           // 1 &lt; 2 for two duplicates
 *           throw new DomainException(ErrorCode.PRODUCT_NOT_FOUND);
 *       }
 *       ...
 *   }
 * </pre>
 * Because the database collapses duplicate ids in the {@code IN (...)}
 * clause, the size comparison is structurally wrong whenever any
 * {@code productId} appears more than once in the request, even though every
 * product exists.
 *
 * <p><b>Test design:</b> Mockito-style unit test that wires the real
 * {@link ProductService} with mocks for {@link ProductRepository},
 * {@link ShopRepository}, {@link CategoryRepository}, and
 * {@link ProductMapper}. The bug lives entirely inside
 * {@code decreaseProducts}, so exercising it directly keeps the failure
 * isolated to the structural premise (size-comparison after a
 * duplicate-collapsing query). The test is colocated with {@code
 * ProductService} in package {@code com.example.shopnow.product} so it can
 * reference the package-private {@link ProductMapper} contract directly.
 *
 * <p><b>Expected counterexample on unfixed code:</b> any list with at least
 * two {@link OrderItemRequest}s sharing a {@code productId} (e.g. two copies
 * of the same id with quantity 1 each) → {@code decreaseProducts(...)}
 * throws {@code DomainException(PRODUCT_NOT_FOUND)} because
 * {@code products.size() == 1 &lt; itemRequests.size() == 2}.
 */
class ProductServiceBug8ExplorationTest {

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

        // decreaseQuantity is mocked to "succeed" so the test focuses solely
        // on the size-comparison check that triggers PRODUCT_NOT_FOUND. Any
        // real DB row with sufficient stock would behave the same way.
        lenient().when(productRepository.decreaseQuantity(any(UUID.class), anyInt())).thenReturn(1);
    }

    /**
     * Scoped property-style coverage: vary the number of duplicate
     * occurrences, the number of additional distinct products in the same
     * request, and the per-item quantity. Every input satisfies C(8) (at
     * least one productId appears ≥ 2 times) and every referenced product is
     * ACTIVE with sufficient stock.
     */
    static Stream<Arguments> duplicateProductIdScenarios() {
        return Stream.of(
                // numDuplicateOccurrences, numDistinctExtraIds, perItemQuantity
                Arguments.of(2, 0, 1),  // minimal counterexample: 2 items sharing 1 id
                Arguments.of(3, 0, 1),
                Arguments.of(2, 1, 1),  // duplicates + one distinct id
                Arguments.of(2, 2, 2),
                Arguments.of(4, 0, 1),
                Arguments.of(2, 0, 5)); // larger per-item quantity (still ≤ stock)
    }

    @ParameterizedTest(name = "[{index}] duplicates={0}, distinctExtras={1}, qty={2}"
            + " -> NOT DomainException(PRODUCT_NOT_FOUND)")
    @MethodSource("duplicateProductIdScenarios")
    @DisplayName("decreaseProducts with duplicate productId SHALL NOT throw DomainException(PRODUCT_NOT_FOUND)")
    void decreaseProducts_withDuplicateProductId_shallNotThrowProductNotFound(
            int numDuplicateOccurrences,
            int numDistinctExtraIds,
            int perItemQuantity) {

        // --- Arrange --------------------------------------------------------
        UUID duplicatedProductId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID shopOwnerId = UUID.randomUUID();

        // The duplicated product: ACTIVE, isDeleted=false, ample stock.
        Product duplicatedProduct = buildActiveProduct(
                duplicatedProductId, shopId, shopOwnerId, /* stock */ 1_000);
        ProductInfoForOrder duplicatedInfo = new ProductInfoForOrder(
                duplicatedProductId,
                BigDecimal.valueOf(10),
                "Duplicated Product",
                duplicatedProduct.getQuantity(),
                shopId,
                shopOwnerId);

        // Optional additional distinct products to stress-test mixed inputs.
        List<Product> distinctExtraProducts = new ArrayList<>();
        List<ProductInfoForOrder> distinctExtraInfos = new ArrayList<>();
        List<UUID> distinctExtraIds = new ArrayList<>();
        for (int i = 0; i < numDistinctExtraIds; i++) {
            UUID extraId = UUID.randomUUID();
            distinctExtraIds.add(extraId);
            Product p = buildActiveProduct(extraId, shopId, shopOwnerId, 100);
            distinctExtraProducts.add(p);
            distinctExtraInfos.add(new ProductInfoForOrder(
                    extraId,
                    BigDecimal.valueOf(5),
                    "Extra " + i,
                    p.getQuantity(),
                    shopId,
                    shopOwnerId));
        }

        // Build the request: duplicates first, then any distinct extras.
        List<OrderItemRequest> items = new ArrayList<>();
        for (int i = 0; i < numDuplicateOccurrences; i++) {
            items.add(new OrderItemRequest(duplicatedProductId, perItemQuantity));
        }
        for (UUID extraId : distinctExtraIds) {
            items.add(new OrderItemRequest(extraId, perItemQuantity));
        }

        // Stub the persistence tier as the real DB would respond:
        // SQL `IN (?, ?)` collapses duplicates, so the result is keyed by id.
        // The product list returned to ProductService.decreaseProducts is
        // therefore strictly smaller than itemRequests when duplicates exist
        // — this is exactly the structural premise the buggy size-comparison
        // relies on.
        List<Product> productsFromDb = new ArrayList<>();
        productsFromDb.add(duplicatedProduct);
        productsFromDb.addAll(distinctExtraProducts);

        List<ProductInfoForOrder> productInfos = new ArrayList<>();
        productInfos.add(duplicatedInfo);
        productInfos.addAll(distinctExtraInfos);

        when(productRepository.findAllWithShopByStatusAndIsDeletedFalseAndIdIn(
                eq(ProductStatus.ACTIVE), ArgumentMatchers.<List<UUID>>any()))
                .thenReturn(productsFromDb);
        lenient().when(productMapper.toProductInfoForOrders(anyList())).thenReturn(productInfos);

        // --- Act ------------------------------------------------------------
        List<OrderLineRequest> lines = items.stream()
                .map(item -> new OrderLineRequest(item.productId(), item.quantity()))
                .toList();
        Throwable thrown = catchThrowable(() -> productService.decreaseProducts(lines));

        // --- Assert ---------------------------------------------------------
        // P(8): regardless of whichever merge-vs-reject policy the fix lands
        // on, PRODUCT_NOT_FOUND is the wrong outcome here — every productId
        // references an existing, ACTIVE, in-stock product. On unfixed code
        // this assertion fails with the precise counterexample
        // (DomainException carrying ErrorCode.PRODUCT_NOT_FOUND).
        boolean isProductNotFound =
                thrown instanceof DomainException de
                        && de.getErrorCode() == ErrorCode.PRODUCT_NOT_FOUND;

        assertThat(isProductNotFound)
                .as("decreaseProducts with %d duplicate occurrence(s) of productId=%s "
                                + "(plus %d distinct extra id(s), qty=%d each) SHALL NOT throw "
                                + "DomainException(PRODUCT_NOT_FOUND); actual thrown=%s",
                        numDuplicateOccurrences,
                        duplicatedProductId,
                        numDistinctExtraIds,
                        perItemQuantity,
                        thrown)
                .isFalse();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Build an ACTIVE, non-deleted {@link Product} with the supplied stock,
     * attached to a {@link Shop} whose {@code ownerId} matches the supplied
     * {@code shopOwnerId}. The id is set via reflection because
     * {@link com.example.shopnow.shared.BaseEntity#getId()} is normally
     * populated by Hibernate.
     */
    private static Product buildActiveProduct(UUID productId, UUID shopId, UUID shopOwnerId, int stock) {
        Shop shop = new Shop();
        ReflectionTestUtils.setField(shop, "id", shopId);
        shop.setOwnerId(shopOwnerId);

        Product p = Product.builder()
                .name("Product-" + productId)
                .price(BigDecimal.valueOf(10))
                .quantity(stock)
                .status(ProductStatus.ACTIVE)
                .shop(shop)
                .isDeleted(false)
                .build();
        ReflectionTestUtils.setField(p, "id", productId);
        return p;
    }
}
