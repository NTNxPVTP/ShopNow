package com.example.shopnow.product;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.shopnow.product.models.Product;
import com.example.shopnow.product.models.ProductStatus;
import com.example.shopnow.product.rest.dto.CreateProductRequest;
import com.example.shopnow.product.rest.dto.ProductDetailResponse;
import com.example.shopnow.product.rest.dto.UpdateProductRequest;
import com.example.shopnow.shared.DomainException;
import com.example.shopnow.shared.ErrorCode;
import com.example.shopnow.shared.PageResponse;
import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true, rollbackFor = Exception.class)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper mapper;

    public Product viewDetailsOfProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new DomainException(ErrorCode.PRODUCT_NOT_FOUND));
        return product;
    }

    // has not have shop owner, category,... yet
    @Transactional
    public ProductDetailResponse createProduct(CreateProductRequest request) {
        Product product = mapper.fromCreateRequestToProduct(request);
        product.setStatus(ProductStatus.ACTIVE);
        product = productRepository.save(product);
        ProductDetailResponse detail = mapper.toDto(product);
        return detail;
    }

    // has not check permission shop owner
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
        System.out.println("before mapping:");
        System.out.println(product);

        mapper.updateProductFromUpdateRequest(request, product);
        System.out.println("after mapping:");
        System.out.println(product);
        productRepository.save(product);
        return mapper.toDto(product);
    }

    //has not find by categories, or any other field...
    public PageResponse<ProductDetailResponse> getProducts(Pageable pageable){
        Page<Product> products = productRepository.findWithPageReponseBy(pageable);
        return mapper.toPageResponse(products);
    }
}
