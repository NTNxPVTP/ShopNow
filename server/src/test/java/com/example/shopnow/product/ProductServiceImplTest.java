package com.example.shopnow.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.product.api.dto.OrderLineRequest;
import com.example.shopnow.product.api.dto.ProductInfoForOrder;
import com.example.shopnow.product.models.Category;
import com.example.shopnow.product.models.Product;
import com.example.shopnow.product.models.ProductStatus;
import com.example.shopnow.product.models.Shop;
import com.example.shopnow.product.rest.dto.CreateProductRequest;
import com.example.shopnow.product.rest.dto.ProductDetailResponse;
import com.example.shopnow.product.rest.dto.UpdateProductRequest;
import com.example.shopnow.shared.PageInfo;
import com.example.shopnow.shared.PageResponse;
import com.example.shopnow.user.api.AuthenticatedUser;

/**
 * Unit test cho {@link ProductServiceImpl} – các kịch bản truy vấn và tạo sản phẩm.
 *
 * <p>Pure unit test theo convention của repo: JUnit 5 + Mockito + AssertJ, không
 * khởi động Spring context. Mọi collaborator đều được mock; logic phân nhánh và
 * điều phối được kiểm tra qua giá trị trả về và tương tác.
 *
 * <p>Lớp test này KHÔNG lặp lại các kịch bản kiểm tra quyền sở hữu đã có trong
 * {@link ProductServiceOwnershipTest}. Bao gồm retrieval + creation (task 2.2) và
 * update/delete/listing/stock-decrement (task 2.3) trong cùng file này.
 *
 * <p>Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10,
 * 1.11, 1.12, 1.13, 1.14, 1.15, 1.16, 1.17, 1.18
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    // ---------- Shared fixture helpers ----------

    /**
     * Set id cho entity dùng {@link ReflectionTestUtils} vì {@code BaseEntity.id}
     * là {@code @GeneratedValue} và không có persistence layer chạy trong unit test.
     */
    private static <T> T withId(T entity, UUID id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    /** Mock {@link AuthenticatedUser} chỉ stub {@code getId()} – dữ liệu duy nhất logic ownership đọc. */
    private static AuthenticatedUser ownerWithId(UUID id) {
        AuthenticatedUser user = Mockito.mock(AuthenticatedUser.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private static Shop shopOwnedBy(UUID shopId, UUID ownerId) {
        Shop shop = new Shop();
        withId(shop, shopId);
        shop.setOwnerId(ownerId);
        shop.setName("Test Shop");
        shop.setIsActive(true);
        return shop;
    }

    private static Product sampleProduct(UUID id) {
        Product product = Product.builder()
                .name("Test Product")
                .price(new BigDecimal("99.99"))
                .quantity(10)
                .pictureUrl("http://example.com/pic.jpg")
                .status(ProductStatus.ACTIVE)
                .isDeleted(false)
                .build();
        if (id != null) {
            withId(product, id);
        }
        return product;
    }

    private static Category categoryWithId(UUID id, String name) {
        Category c = new Category();
        c.setName(name);
        withId(c, id);
        return c;
    }

    private static CreateProductRequest createRequest(UUID shopId, Set<UUID> categoryIds) {
        return new CreateProductRequest(
                "Test Product",
                "http://example.com/pic.jpg",
                10,
                new BigDecimal("99.99"),
                shopId,
                categoryIds);
    }

    private static ProductDetailResponse detailResponse(UUID productId, UUID shopId, Set<UUID> categoryIds) {
        return new ProductDetailResponse(
                productId,
                "Test Product",
                "http://example.com/pic.jpg",
                10,
                new BigDecimal("99.99"),
                ProductStatus.ACTIVE,
                shopId,
                categoryIds == null ? Set.of() : categoryIds);
    }

    // ---------- viewDetailsOfProduct ----------

    @Nested
    @DisplayName("viewDetailsOfProduct")
    class ViewDetailsOfProduct {

        @Test
        @DisplayName("trả về DTO khi product tồn tại và ACTIVE")
        void returnsMappedDto_whenProductFoundAndActive() {
            // Arrange
            UUID productId = UUID.randomUUID();
            UUID shopId = UUID.randomUUID();
            Product product = sampleProduct(productId);
            ProductDetailResponse expected = detailResponse(productId, shopId, Set.of());

            when(productRepository.findByIdAndStatusAndIsDeletedFalse(productId, ProductStatus.ACTIVE))
                    .thenReturn(Optional.of(product));
            when(productMapper.toDto(product)).thenReturn(expected);

            // Act
            ProductDetailResponse actual = productService.viewDetailsOfProduct(productId);

            // Assert
            assertThat(actual).isSameAs(expected);
            verify(productMapper).toDto(product);
        }

        @Test
        @DisplayName("ném PRODUCT_NOT_FOUND khi không có product ACTIVE phù hợp")
        void throwsProductNotFound_whenAbsent() {
            // Arrange
            UUID productId = UUID.randomUUID();
            when(productRepository.findByIdAndStatusAndIsDeletedFalse(productId, ProductStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.viewDetailsOfProduct(productId))
                    .isInstanceOf(DomainException.class)
                    .extracting(ex -> ((DomainException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);

            verifyNoInteractions(productMapper);
        }
    }

    // ---------- createProduct ----------

    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        @DisplayName("ném SHOP_NOT_FOUND và KHÔNG save khi shop không tồn tại")
        void throwsShopNotFound_andNeverSaves_whenShopMissing() {
            // Arrange
            UUID shopId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            CreateProductRequest request = createRequest(shopId, null);

            when(productMapper.fromCreateRequestToProduct(request)).thenReturn(sampleProduct(null));
            when(shopRepository.findById(shopId)).thenReturn(Optional.empty());

            AuthenticatedUser owner = Mockito.mock(AuthenticatedUser.class); // getId() không cần thiết ở nhánh này
            // không stub owner.getId() để tránh unnecessary stubbing; STRICT_STUBS

            // Act & Assert
            assertThatThrownBy(() -> productService.createProduct(request, owner))
                    .isInstanceOf(DomainException.class)
                    .extracting(ex -> ((DomainException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.SHOP_NOT_FOUND);

            verify(productRepository, never()).save(any(Product.class));
            verifyNoInteractions(categoryRepository);
            // ownerId chỉ để tránh unused warning trong fixture
            assertThat(ownerId).isNotNull();
        }

        @Test
        @DisplayName("ném PRODUCT_ACCESS_DENIED và KHÔNG save khi owner.getId() null")
        void throwsAccessDenied_andNeverSaves_whenOwnerIdNull() {
            // Arrange
            UUID shopId = UUID.randomUUID();
            UUID shopOwnerId = UUID.randomUUID();
            CreateProductRequest request = createRequest(shopId, null);

            lenient().when(productMapper.fromCreateRequestToProduct(request)).thenReturn(sampleProduct(null));
            when(shopRepository.findById(shopId)).thenReturn(Optional.of(shopOwnedBy(shopId, shopOwnerId)));

            AuthenticatedUser owner = Mockito.mock(AuthenticatedUser.class);
            when(owner.getId()).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> productService.createProduct(request, owner))
                    .isInstanceOf(DomainException.class)
                    .extracting(ex -> ((DomainException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.PRODUCT_ACCESS_DENIED);

            verify(productRepository, never()).save(any(Product.class));
            verifyNoInteractions(categoryRepository);
        }

        @Test
        @DisplayName("ném PRODUCT_ACCESS_DENIED và KHÔNG save khi owner.getId() khác shop.ownerId")
        void throwsAccessDenied_andNeverSaves_whenOwnerIdMismatch() {
            // Arrange
            UUID shopId = UUID.randomUUID();
            UUID shopOwnerId = UUID.randomUUID();
            UUID otherOwnerId = UUID.randomUUID();
            CreateProductRequest request = createRequest(shopId, null);

            lenient().when(productMapper.fromCreateRequestToProduct(request)).thenReturn(sampleProduct(null));
            when(shopRepository.findById(shopId)).thenReturn(Optional.of(shopOwnedBy(shopId, shopOwnerId)));

            AuthenticatedUser owner = ownerWithId(otherOwnerId);

            // Act & Assert
            assertThatThrownBy(() -> productService.createProduct(request, owner))
                    .isInstanceOf(DomainException.class)
                    .extracting(ex -> ((DomainException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.PRODUCT_ACCESS_DENIED);

            verify(productRepository, never()).save(any(Product.class));
            verifyNoInteractions(categoryRepository);
        }

        @Test
        @DisplayName("save product với status ACTIVE + categories đã resolve khi mọi categoryId hợp lệ")
        void savesProductWithActiveStatusAndResolvedCategories_whenValid() {
            // Arrange
            UUID shopId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            UUID savedProductId = UUID.randomUUID();
            UUID catId1 = UUID.randomUUID();
            UUID catId2 = UUID.randomUUID();
            Set<UUID> categoryIds = new HashSet<>(Set.of(catId1, catId2));
            CreateProductRequest request = createRequest(shopId, categoryIds);

            Shop shop = shopOwnedBy(shopId, ownerId);
            Product mapped = sampleProduct(null); // chưa có status, chưa có shop
            mapped.setStatus(null);

            Set<Category> resolvedCategories = Set.of(
                    categoryWithId(catId1, "A"),
                    categoryWithId(catId2, "B"));

            ProductDetailResponse expected = detailResponse(savedProductId, shopId, Set.of(catId1, catId2));

            when(productMapper.fromCreateRequestToProduct(request)).thenReturn(mapped);
            when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
            when(categoryRepository.findAllByIdIn(any())).thenReturn(resolvedCategories);
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
                Product arg = inv.getArgument(0);
                withId(arg, savedProductId);
                return arg;
            });
            when(productMapper.toDto(any(Product.class))).thenReturn(expected);

            AuthenticatedUser owner = ownerWithId(ownerId);

            // Act
            ProductDetailResponse actual = productService.createProduct(request, owner);

            // Assert: response trả đúng DTO mapper
            assertThat(actual).isSameAs(expected);

            // Assert: capture entity được save và kiểm tra trạng thái + categories
            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository, times(1)).save(captor.capture());
            Product saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(ProductStatus.ACTIVE);
            assertThat(saved.getShop()).isSameAs(shop);
            assertThat(saved.getCategories())
                    .containsExactlyInAnyOrderElementsOf(resolvedCategories);

            // Assert: category resolution sử dụng đúng tập id yêu cầu
            ArgumentCaptor<Set<UUID>> idsCaptor = ArgumentCaptor.forClass(Set.class);
            verify(categoryRepository).findAllByIdIn(idsCaptor.capture());
            assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(catId1, catId2);
        }

        @Test
        @DisplayName("ném CATEGORY_NOT_FOUND và KHÔNG save khi có categoryId không tồn tại")
        void throwsCategoryNotFound_andNeverSaves_whenAnyCategoryUnknown() {
            // Arrange
            UUID shopId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            UUID catId1 = UUID.randomUUID();
            UUID catId2 = UUID.randomUUID();
            Set<UUID> categoryIds = new HashSet<>(Set.of(catId1, catId2));
            CreateProductRequest request = createRequest(shopId, categoryIds);

            Shop shop = shopOwnedBy(shopId, ownerId);
            Product mapped = sampleProduct(null);

            // findAllByIdIn trả về ít hơn số id yêu cầu => một id không hợp lệ
            Set<Category> partial = Set.of(categoryWithId(catId1, "A"));

            when(productMapper.fromCreateRequestToProduct(request)).thenReturn(mapped);
            when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
            when(categoryRepository.findAllByIdIn(any())).thenReturn(partial);

            AuthenticatedUser owner = ownerWithId(ownerId);

            // Act & Assert
            assertThatThrownBy(() -> productService.createProduct(request, owner))
                    .isInstanceOf(DomainException.class)
                    .extracting(ex -> ((DomainException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);

            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("save product mà KHÔNG truy vấn category khi categoryIds null")
        void savesWithoutResolvingCategories_whenCategoryIdsNull() {
            // Arrange
            UUID shopId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            UUID savedProductId = UUID.randomUUID();
            CreateProductRequest request = createRequest(shopId, null);

            Shop shop = shopOwnedBy(shopId, ownerId);
            Product mapped = sampleProduct(null);
            ProductDetailResponse expected = detailResponse(savedProductId, shopId, Set.of());

            when(productMapper.fromCreateRequestToProduct(request)).thenReturn(mapped);
            when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
                Product arg = inv.getArgument(0);
                withId(arg, savedProductId);
                return arg;
            });
            when(productMapper.toDto(any(Product.class))).thenReturn(expected);

            AuthenticatedUser owner = ownerWithId(ownerId);

            // Act
            ProductDetailResponse actual = productService.createProduct(request, owner);

            // Assert: thành công không throw
            assertThat(actual).isSameAs(expected);

            // Assert: category repository không bị truy vấn
            verifyNoInteractions(categoryRepository);

            // Assert: status ACTIVE và shop được gán đúng
            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(captor.capture());
            Product saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(ProductStatus.ACTIVE);
            assertThat(saved.getShop()).isSameAs(shop);
            // categories không được set bởi service ở nhánh này (resolveCategories trả Set.of())
        }

        @Test
        @DisplayName("save product mà KHÔNG truy vấn category khi categoryIds rỗng")
        void savesWithoutResolvingCategories_whenCategoryIdsEmpty() {
            // Arrange
            UUID shopId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            UUID savedProductId = UUID.randomUUID();
            CreateProductRequest request = createRequest(shopId, Set.of());

            Shop shop = shopOwnedBy(shopId, ownerId);
            Product mapped = sampleProduct(null);
            ProductDetailResponse expected = detailResponse(savedProductId, shopId, Set.of());

            when(productMapper.fromCreateRequestToProduct(request)).thenReturn(mapped);
            when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
                Product arg = inv.getArgument(0);
                withId(arg, savedProductId);
                return arg;
            });
            when(productMapper.toDto(any(Product.class))).thenReturn(expected);

            AuthenticatedUser owner = ownerWithId(ownerId);

            // Act
            ProductDetailResponse actual = productService.createProduct(request, owner);

            // Assert: thành công không throw
            assertThat(actual).isSameAs(expected);
            verifyNoInteractions(categoryRepository);
            verify(productRepository, times(1)).save(any(Product.class));
            // Đối chiếu eq để đảm bảo findById dùng đúng shopId
            verify(shopRepository).findById(eq(shopId));
        }
    }

    // ---------- deleteProduct ----------

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProduct {

        @Test
        @DisplayName("ném USER_NOT_FOUND và KHÔNG soft-delete khi owner.getId() null")
        void throwsUserNotFound_andNeverSoftDeletes_whenOwnerIdNull() {
            // Arrange
            UUID productId = UUID.randomUUID();
            AuthenticatedUser owner = Mockito.mock(AuthenticatedUser.class);
            when(owner.getId()).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> productService.deleteProduct(productId, owner))
                    .isInstanceOf(DomainException.class)
                    .extracting(ex -> ((DomainException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);

            verify(productRepository, never()).softDeleteProductByIdAndShopOwnerId(any(), any());
        }

        @Test
        @DisplayName("ném PRODUCT_NOT_FOUND khi soft-delete ảnh hưởng 0 dòng")
        void throwsProductNotFound_whenZeroRowsAffected() {
            // Arrange
            UUID productId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            AuthenticatedUser owner = ownerWithId(ownerId);
            when(productRepository.softDeleteProductByIdAndShopOwnerId(productId, ownerId)).thenReturn(0);

            // Act & Assert
            assertThatThrownBy(() -> productService.deleteProduct(productId, owner))
                    .isInstanceOf(DomainException.class)
                    .extracting(ex -> ((DomainException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        }

        @Test
        @DisplayName("trả về chuỗi xác nhận chứa id khi soft-delete ảnh hưởng >=1 dòng")
        void returnsConfirmation_whenAtLeastOneRowAffected() {
            // Arrange
            UUID productId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            AuthenticatedUser owner = ownerWithId(ownerId);
            when(productRepository.softDeleteProductByIdAndShopOwnerId(productId, ownerId)).thenReturn(1);

            // Act
            String result = productService.deleteProduct(productId, owner);

            // Assert
            assertThat(result).contains(productId.toString());
        }
    }

    // ---------- updateProduct ----------

    @Nested
    @DisplayName("updateProduct")
    class UpdateProduct {

        private static UpdateProductRequest updateRequest() {
            return new UpdateProductRequest(
                    "Updated Name",
                    "http://example.com/new.jpg",
                    20,
                    new BigDecimal("199.99"));
        }

        @Test
        @DisplayName("ném PRODUCT_NOT_FOUND khi product không tồn tại")
        void throwsProductNotFound_whenAbsent() {
            // Arrange
            UUID productId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            UpdateProductRequest request = updateRequest();
            AuthenticatedUser owner = ownerWithId(ownerId);

            when(productRepository.findWithShopById(productId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.updateProduct(request, productId, owner))
                    .isInstanceOf(DomainException.class)
                    .extracting(ex -> ((DomainException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);

            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("ném PRODUCT_ACCESS_DENIED và KHÔNG save khi owner.getId() null")
        void throwsAccessDenied_andNeverSaves_whenOwnerIdNull() {
            // Arrange
            UUID productId = UUID.randomUUID();
            UUID shopId = UUID.randomUUID();
            UUID shopOwnerId = UUID.randomUUID();
            UpdateProductRequest request = updateRequest();

            Product product = sampleProduct(productId);
            product.setShop(shopOwnedBy(shopId, shopOwnerId));

            when(productRepository.findWithShopById(productId)).thenReturn(Optional.of(product));

            AuthenticatedUser owner = Mockito.mock(AuthenticatedUser.class);
            when(owner.getId()).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> productService.updateProduct(request, productId, owner))
                    .isInstanceOf(DomainException.class)
                    .extracting(ex -> ((DomainException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.PRODUCT_ACCESS_DENIED);

            verify(productRepository, never()).save(any(Product.class));
            verify(productMapper, never()).updateProductFromUpdateRequest(any(), any());
        }

        @Test
        @DisplayName("ném PRODUCT_ACCESS_DENIED và KHÔNG save khi owner.getId() khác shop.ownerId")
        void throwsAccessDenied_andNeverSaves_whenOwnerIdMismatch() {
            // Arrange
            UUID productId = UUID.randomUUID();
            UUID shopId = UUID.randomUUID();
            UUID shopOwnerId = UUID.randomUUID();
            UUID otherOwnerId = UUID.randomUUID();
            UpdateProductRequest request = updateRequest();

            Product product = sampleProduct(productId);
            product.setShop(shopOwnedBy(shopId, shopOwnerId));

            when(productRepository.findWithShopById(productId)).thenReturn(Optional.of(product));

            AuthenticatedUser owner = ownerWithId(otherOwnerId);

            // Act & Assert
            assertThatThrownBy(() -> productService.updateProduct(request, productId, owner))
                    .isInstanceOf(DomainException.class)
                    .extracting(ex -> ((DomainException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.PRODUCT_ACCESS_DENIED);

            verify(productRepository, never()).save(any(Product.class));
            verify(productMapper, never()).updateProductFromUpdateRequest(any(), any());
        }

        @Test
        @DisplayName("áp dụng mapper update + save + trả DTO khi owner hợp lệ")
        void appliesMapperUpdateSavesAndReturnsDto_whenValid() {
            // Arrange
            UUID productId = UUID.randomUUID();
            UUID shopId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            UpdateProductRequest request = updateRequest();

            Product product = sampleProduct(productId);
            product.setShop(shopOwnedBy(shopId, ownerId));

            ProductDetailResponse expected = detailResponse(productId, shopId, Set.of());

            when(productRepository.findWithShopById(productId)).thenReturn(Optional.of(product));
            when(productMapper.toDto(product)).thenReturn(expected);

            AuthenticatedUser owner = ownerWithId(ownerId);

            // Act
            ProductDetailResponse actual = productService.updateProduct(request, productId, owner);

            // Assert
            assertThat(actual).isSameAs(expected);
            verify(productMapper).updateProductFromUpdateRequest(request, product);
            verify(productRepository).save(product);
            verify(productMapper).toDto(product);
        }
    }

    // ---------- getProducts (listing) ----------

    @Nested
    @DisplayName("getProducts")
    class GetProducts {

        @Test
        @DisplayName("trả về PageResponse khi inStockOnly=false")
        void returnsMappedPageResponse_whenInStockOnlyFalse() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Product product = sampleProduct(UUID.randomUUID());
            Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);
            PageResponse<ProductDetailResponse> expected = new PageResponse<>(
                    List.of(detailResponse(UUID.randomUUID(), UUID.randomUUID(), Set.of())),
                    PageInfo.builder()
                            .pageNumber(0).pageSize(10).totalPages(1).isLast(true).totalElements(1)
                            .build());

            when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
            when(productMapper.toPageResponse(page)).thenReturn(expected);

            // Act
            PageResponse<ProductDetailResponse> actual = productService.getProducts(
                    pageable, null, null, null, null, null, false);

            // Assert
            assertThat(actual).isSameAs(expected);
            verify(productRepository).findAll(any(Specification.class), eq(pageable));
            verify(productMapper).toPageResponse(page);
        }

        @Test
        @DisplayName("trả về PageResponse khi inStockOnly=true")
        void returnsMappedPageResponse_whenInStockOnlyTrue() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Product product = sampleProduct(UUID.randomUUID());
            Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);
            PageResponse<ProductDetailResponse> expected = new PageResponse<>(
                    List.of(detailResponse(UUID.randomUUID(), UUID.randomUUID(), Set.of())),
                    PageInfo.builder()
                            .pageNumber(0).pageSize(10).totalPages(1).isLast(true).totalElements(1)
                            .build());

            when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
            when(productMapper.toPageResponse(page)).thenReturn(expected);

            // Act
            PageResponse<ProductDetailResponse> actual = productService.getProducts(
                    pageable, null, null, null, null, null, true);

            // Assert
            assertThat(actual).isSameAs(expected);
            verify(productRepository).findAll(any(Specification.class), eq(pageable));
            verify(productMapper).toPageResponse(page);
        }
    }

    // ---------- decreaseProducts (stock decrement) ----------

    @Nested
    @DisplayName("decreaseProducts")
    class DecreaseProducts {

        private static OrderLineRequest line(UUID productId, int quantity) {
            return new OrderLineRequest(productId, quantity);
        }

        @Test
        @DisplayName("ném PRODUCT_NOT_FOUND khi số product tìm được ít hơn số dòng yêu cầu")
        void throwsProductNotFound_whenFewerProductsFound() {
            // Arrange
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            List<OrderLineRequest> lines = List.of(line(id1, 1), line(id2, 2));

            // chỉ tìm được 1 product cho 2 dòng
            List<Product> found = List.of(sampleProduct(id1));
            when(productRepository.findAllWithShopByStatusAndIsDeletedFalseAndIdIn(
                    eq(ProductStatus.ACTIVE), any())).thenReturn(found);

            // Act & Assert
            assertThatThrownBy(() -> productService.decreaseProducts(lines))
                    .isInstanceOf(DomainException.class)
                    .extracting(ex -> ((DomainException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);

            verify(productRepository, never()).decreaseQuantity(any(), org.mockito.ArgumentMatchers.anyInt());
        }

        @Test
        @DisplayName("ném PRODUCT_OUT_OF_STOCK khi một dòng decreaseQuantity trả 0")
        void throwsOutOfStock_whenALineReturnsZero() {
            // Arrange
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            List<OrderLineRequest> lines = List.of(line(id1, 1), line(id2, 2));

            List<Product> found = List.of(sampleProduct(id1), sampleProduct(id2));
            when(productRepository.findAllWithShopByStatusAndIsDeletedFalseAndIdIn(
                    eq(ProductStatus.ACTIVE), any())).thenReturn(found);
            when(productRepository.decreaseQuantity(id1, 1)).thenReturn(1);
            when(productRepository.decreaseQuantity(id2, 2)).thenReturn(0);

            // Act & Assert
            assertThatThrownBy(() -> productService.decreaseProducts(lines))
                    .isInstanceOf(DomainException.class)
                    .extracting(ex -> ((DomainException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.PRODUCT_OUT_OF_STOCK);

            verifyNoInteractions(productMapper);
        }

        @Test
        @DisplayName("trả danh sách đã map và gọi decreaseQuantity đúng mỗi dòng khi mọi dòng thành công")
        void returnsMappedListAndDecrementsEachLine_whenAllSucceed() {
            // Arrange
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            List<OrderLineRequest> lines = List.of(line(id1, 1), line(id2, 2));

            List<Product> found = List.of(sampleProduct(id1), sampleProduct(id2));
            List<ProductInfoForOrder> expected = List.of(
                    new ProductInfoForOrder(id1, new BigDecimal("99.99"), "P1", 1,
                            UUID.randomUUID(), UUID.randomUUID()),
                    new ProductInfoForOrder(id2, new BigDecimal("49.99"), "P2", 2,
                            UUID.randomUUID(), UUID.randomUUID()));

            when(productRepository.findAllWithShopByStatusAndIsDeletedFalseAndIdIn(
                    eq(ProductStatus.ACTIVE), any())).thenReturn(found);
            when(productRepository.decreaseQuantity(id1, 1)).thenReturn(1);
            when(productRepository.decreaseQuantity(id2, 2)).thenReturn(1);
            when(productMapper.toProductInfoForOrders(found)).thenReturn(expected);

            // Act
            List<ProductInfoForOrder> actual = productService.decreaseProducts(lines);

            // Assert
            assertThat(actual).isSameAs(expected);
            verify(productRepository, times(lines.size()))
                    .decreaseQuantity(any(), org.mockito.ArgumentMatchers.anyInt());
            verify(productMapper).toProductInfoForOrders(found);
        }
    }
}
