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
import com.example.shopnow.order.rest.dto.OrderItemRequest;
import com.example.shopnow.product.api.dto.ProductInfoForOrder;
import com.example.shopnow.product.models.Category;
import com.example.shopnow.product.models.Product;
import com.example.shopnow.product.models.ProductStatus;
import com.example.shopnow.product.models.Shop;
import com.example.shopnow.product.rest.dto.CreateProductRequest;
import com.example.shopnow.product.rest.dto.ProductDetailResponse;
import com.example.shopnow.product.rest.dto.UpdateProductRequest;
import com.example.shopnow.shared.PageResponse;
import com.example.shopnow.user.models.User;
import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true, rollbackFor = Exception.class)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    public ProductDetailResponse viewDetailsOfProduct(UUID id) {

        Product product = productRepository.findByIdAndStatusAndIsDeletedFalse(id, ProductStatus.ACTIVE)
                .orElseThrow(() -> new DomainException(ErrorCode.PRODUCT_NOT_FOUND));
        return productMapper.toDto(product);
    }

    // has not have shop owner, category,... yet
    @Transactional
    public ProductDetailResponse createProduct(CreateProductRequest request, User owner) {
        Product product = productMapper.fromCreateRequestToProduct(request);
        Shop shop = shopRepository.findById(request.shopId())
                .orElseThrow(() -> new DomainException(ErrorCode.SHOP_NOT_FOUND));

        UUID shopOwnerId = owner.getId();
        if (shopOwnerId == null || !shopOwnerId.equals(shop.getOwnerId())) {
            throw new DomainException(ErrorCode.PRODUCT_ACCESS_DENIED);
        }

        Set<Category> categories = resolveCategories(request.categoryIds());

        product.setShop(shop);
        product.setStatus(ProductStatus.ACTIVE);

        if (!categories.isEmpty()) {
            product.setCategories(categories);
        }

        product = productRepository.save(product);
        ProductDetailResponse detail = productMapper.toDto(product);
        return detail;
    }

    // TODO:
    // business logic: if product is in order, set status = INACTIVE, else delete
    @Transactional
    public String deleteProduct(UUID id, User owner) {
        UUID shopOwnerId = owner.getId();
        if (shopOwnerId == null) {
            throw new DomainException(ErrorCode.USER_NOT_FOUND);
        }
        int check = productRepository.softDeleteProductByIdAndShopOwnerId(id, shopOwnerId);
        if (check == 0) {
            throw new DomainException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return "Deleted product with id: " + id;
    }

    @Transactional
    public ProductDetailResponse updateProduct(UpdateProductRequest request, UUID produdctId, User owner) {
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

        Page<Product> products = productRepository.findAll(specification, pageable);
        return productMapper.toPageResponse(products);
    }

    private List<Product> getProducts(List<UUID> ids) {
        List<Product> products = productRepository.findAllWithShopByStatusAndIsDeletedFalseAndIdIn(ProductStatus.ACTIVE,
                ids);
        return products;
    }

    @Transactional
    public List<ProductInfoForOrder> decreaseProducts(List<OrderItemRequest> itemRequests) {
        List<UUID> ids = itemRequests.stream()
                .map(OrderItemRequest::productId)
                .toList();
        List<Product> products = getProducts(ids);

        if (products.size() < itemRequests.size()) {
            throw new DomainException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        for (OrderItemRequest req : itemRequests) {
            int success = productRepository.decreaseQuantity(req.productId(), req.quantity());
            if (success == 0) {
                throw new DomainException(ErrorCode.PRODUCT_OUT_OF_STOCK);
            }

        }
        return productMapper.toProductInfoForOrders(products);
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
