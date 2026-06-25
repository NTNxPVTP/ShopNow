package com.example.shopnow.product.application.usecases;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.product.ProductMapper;
import com.example.shopnow.product.api.dto.OrderLineRequest;
import com.example.shopnow.product.api.dto.ProductInfoForOrder;
import com.example.shopnow.product.domain.models.Product;
import com.example.shopnow.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetProductsInfoUseCase {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public List<ProductInfoForOrder> execute(List<OrderLineRequest> lines) {
        List<UUID> ids = lines.stream()
                .map(OrderLineRequest::productId)
                .toList();
        List<Product> products = productRepository.findActiveWithShopByIds(ids);

        if (products.size() < lines.size()) {
            throw new DomainException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        for (OrderLineRequest req : lines) {
            Product p = products.stream()
                    .filter(prod -> prod.getId().equals(req.productId()))
                    .findFirst()
                    .orElseThrow(() -> new DomainException(ErrorCode.PRODUCT_NOT_FOUND));
            if (p.getQuantity() < req.quantity()) {
                throw new DomainException(ErrorCode.PRODUCT_OUT_OF_STOCK);
            }
        }
        
        return productMapper.toProductInfoForOrders(products);
    }
}
