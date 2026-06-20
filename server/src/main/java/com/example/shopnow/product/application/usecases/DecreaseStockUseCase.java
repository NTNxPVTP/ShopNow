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

/**
 * Use case (driving port) standing behind {@code ProductApi.decreaseProducts}.
 *
 * <p>Exposes a single write execution entry point,
 * {@link #execute(List)}. It validates that every requested product exists and
 * is {@code ACTIVE}, then decreases each product's stock.
 *
 * <p>The stock decrease itself is the atomic, race-free
 * {@code UPDATE ... WHERE quantity >= :qty} delegated through
 * {@link ProductRepository#decreaseQuantity(UUID, int)} (the in-memory
 * {@code Product.decreaseStock} domain guard is intentionally NOT used here so
 * concurrent decrements stay correct). When that atomic update affects 0 rows
 * (insufficient stock) {@link ErrorCode#PRODUCT_OUT_OF_STOCK} is thrown and the
 * whole {@code @Transactional} boundary rolls back, leaving stock unchanged
 * (Requirement 10.2). This preserves the exact behavior previously implemented
 * in {@code ProductServiceImpl.decreaseProducts}.
 */
@Service
@RequiredArgsConstructor
public class DecreaseStockUseCase {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional(rollbackFor = Exception.class)
    public List<ProductInfoForOrder> execute(List<OrderLineRequest> lines) {
        List<UUID> ids = lines.stream()
                .map(OrderLineRequest::productId)
                .toList();
        List<Product> products = productRepository.findActiveWithShopByIds(ids);

        if (products.size() < lines.size()) {
            throw new DomainException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        for (OrderLineRequest req : lines) {
            int success = productRepository.decreaseQuantity(req.productId(), req.quantity());
            if (success == 0) {
                throw new DomainException(ErrorCode.PRODUCT_OUT_OF_STOCK);
            }
        }
        return productMapper.toProductInfoForOrders(products);
    }
}
