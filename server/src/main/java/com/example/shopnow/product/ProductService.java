package com.example.shopnow.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.order.rest.dto.OrderItemRequest;
import com.example.shopnow.product.api.dto.ProductInfoForOrder;
import com.example.shopnow.product.models.Product;
import com.example.shopnow.product.models.ProductStatus;
import com.example.shopnow.product.rest.dto.CreateProductRequest;
import com.example.shopnow.product.rest.dto.ProductDetailResponse;
import com.example.shopnow.product.rest.dto.UpdateProductRequest;
import com.example.shopnow.shared.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true, rollbackFor = Exception.class)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    public ProductDetailResponse viewDetailsOfProduct(UUID id) {

        Product product = productRepository.findByIdAndStatus(id, ProductStatus.ACTIVE)
                .orElseThrow(()-> new DomainException(ErrorCode.PRODUCT_NOT_FOUND));
        return productMapper.toDto(product);
    }

    // has not have shop owner, category,... yet
    @Transactional
    public ProductDetailResponse createProduct(CreateProductRequest request) {
        Product product = productMapper.fromCreateRequestToProduct(request);
        product.setStatus(ProductStatus.ACTIVE);
        product = productRepository.save(product);
        ProductDetailResponse detail = productMapper.toDto(product);
        return detail;
    }

    //TODO: 
    // has not check permission shop owner
    //soft delete
    @Transactional
    public String deleteProduct(UUID id) {
        int check = productRepository.deleteProductById(id);
        if (check == 0) {
            throw new DomainException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return "Delete product successfully!";
    }

    // has not check permission
    @Transactional
    public ProductDetailResponse updateProduct(UpdateProductRequest request, UUID produdctId) {
        Product product = productRepository.findById(produdctId)
                .orElseThrow(() -> new DomainException(ErrorCode.PRODUCT_NOT_FOUND));
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
        boolean inStockOnly
    ) {
        Specification<Product> specification = Specification
            .where(ProductSpecification.hasStatus(ProductStatus.ACTIVE))
            .and(ProductSpecification.hasShopId(shopId))
            .and(ProductSpecification.hasCategoryId(categoryId))
            .and(ProductSpecification.hasNameLike(keyword))
            .and(ProductSpecification.hasPriceGreaterThanOrEqual(minPrice))
            .and(ProductSpecification.hasPriceLessThanOrEqual(maxPrice));

        if (inStockOnly) {
            specification = specification.and(ProductSpecification.isInStock());
        }

        Page<Product> products = productRepository.findAll(specification, pageable);
        return productMapper.toPageResponse(products);
    }

    private List<Product> getProducts(List<UUID> ids){
        List<Product> products = productRepository.findAllWithShopByStatusAndIdIn( ProductStatus.ACTIVE, ids);
        return products;
    }

    @Transactional
    public List<ProductInfoForOrder> decreaseProducts(List<OrderItemRequest> itemRequests){
        List<UUID> ids = itemRequests.stream()
            .map(OrderItemRequest::productId)
            .toList();
        List<Product> products = getProducts(ids);

        if(products.size() < itemRequests.size()){
            throw new DomainException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        for(OrderItemRequest req : itemRequests){
            int success = productRepository.decreaseQuantity(req.productId(), req.quantity());
            if (success ==0) {
                throw new DomainException(ErrorCode.PRODUCT_OUT_OF_STOCK);
            }

        }
        return productMapper.toProductInfoForOrders(products);
    }

}
