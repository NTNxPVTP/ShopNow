package com.example.shopnow.product;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.product.api.ProductApi;
import com.example.shopnow.product.api.dto.OrderLineRequest;
import com.example.shopnow.product.api.dto.ProductInfoForOrder;
import com.example.shopnow.product.application.dto.CreateProductRequest;
import com.example.shopnow.product.application.dto.ProductDetailResponse;
import com.example.shopnow.product.application.dto.UpdateProductRequest;
import com.example.shopnow.product.application.usecases.DecreaseStockUseCase;
import com.example.shopnow.product.domain.models.Category;
import com.example.shopnow.product.domain.models.Product;
import com.example.shopnow.product.domain.models.ProductStatus;
import com.example.shopnow.product.domain.models.Shop;
import com.example.shopnow.product.domain.repository.ProductRepository;
import com.example.shopnow.product.infrastructure.persistence.ProductJpaRepository;
import com.example.shopnow.product.infrastructure.persistence.ProductSpecification;
import com.example.shopnow.product.infrastructure.persistence.CategoryRepository;
import com.example.shopnow.shared.PageResponse;
import com.example.shopnow.user.api.AuthenticatedUser;
import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true, rollbackFor = Exception.class)
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductApi {

    // Domain driven port (technology-neutral): used for save / lookups / soft
    // delete / atomic stock decrease.
    private final ProductRepository productRepository;
    // Spring Data repository kept for the paged Specification read only. This
    // paged read moves into ListProductsUseCase in a later task (8.2); until
    // then ProductServiceImpl talks to it directly so paging behavior stays
    // identical.
    private final ProductJpaRepository productJpaRepository;
    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    // Use case (driving port) standing behind ProductApi.decreaseProducts.
    // decreaseProducts now delegates here so the stock-decrease logic lives in
    // one place (task 8.3); the ProductApi contract stays unchanged.
    private final DecreaseStockUseCase decreaseStockUseCase;

    public ProductDetailResponse viewDetailsOfProduct(UUID id) {

        Product product = productRepository.findActiveById(id)
                .orElseThrow(() -> new DomainException(ErrorCode.PRODUCT_NOT_FOUND));
        return productMapper.toDto(product);
    }



    // TODO:
    // business logic: if product is in order, set status = INACTIVE, else delete
    @Transactional
    public String deleteProduct(UUID id, AuthenticatedUser owner) {
        UUID shopOwnerId = owner.getId();
        if (shopOwnerId == null) {
            throw new DomainException(ErrorCode.USER_NOT_FOUND);
        }
        int check = productRepository.softDelete(id, shopOwnerId);
        if (check == 0) {
            throw new DomainException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return "Deleted product with id: " + id;
    }

    @Transactional
    public ProductDetailResponse updateProduct(UpdateProductRequest request, UUID produdctId, AuthenticatedUser owner) {
        UUID shopOwnerId = owner.getId();
        Product product = productRepository.findWithShopById(produdctId)
                .orElseThrow(() -> new DomainException(ErrorCode.PRODUCT_NOT_FOUND));
        if (shopOwnerId == null || !shopOwnerId.equals(product.getShop().getOwnerId())) {
            throw new DomainException(ErrorCode.PRODUCT_ACCESS_DENIED);
        }
        productMapper.updateProductFromUpdateRequest(request, product);
        productRepository.save(product);
        return productMapper.toDto(product);
    }

    public PageResponse<ProductDetailResponse> getProducts(
            Pageable pageable,
            UUID shopId,
            UUID categoryId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            boolean inStockOnly) {
        Specification<Product> specification = Specification
                .where(ProductSpecification.hasStatus(ProductStatus.ACTIVE))
                .and(ProductSpecification.hasShopId(shopId))
                .and(ProductSpecification.hasCategoryId(categoryId))
                .and(ProductSpecification.hasNameLike(keyword))
                .and(ProductSpecification.hasPriceGreaterThanOrEqual(minPrice))
                .and(ProductSpecification.hasPriceLessThanOrEqual(maxPrice))
                .and(ProductSpecification.isNotDeleted());

        if (inStockOnly) {
            specification = specification.and(ProductSpecification.isInStock());
        }

        Page<Product> products = productJpaRepository.findAll(specification, pageable);
        return productMapper.toPageResponse(products);
    }

    @Override
    @Transactional
    public List<ProductInfoForOrder> decreaseProducts(List<OrderLineRequest> lines) {
        return decreaseStockUseCase.execute(lines);
    }

    private Set<Category> resolveCategories(Set<UUID> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Set.of();
        }

        Set<UUID> uniqueIds = new LinkedHashSet<>(categoryIds);
        Set<Category> categories = categoryRepository.findAllByIdIn(uniqueIds);
        if (categories.size() != uniqueIds.size()) {
            throw new DomainException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        return categories;
    }

}
