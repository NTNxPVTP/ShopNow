package com.example.shopnow.product.api;

import com.example.shopnow.product.api.dto.OrderLineRequest;
import com.example.shopnow.product.api.dto.ProductInfoForOrder;
import java.util.List;

public interface ProductApi {
    List<ProductInfoForOrder> decreaseProducts(List<OrderLineRequest> lines);
    List<ProductInfoForOrder> getProductsInfo(List<OrderLineRequest> lines);
}
