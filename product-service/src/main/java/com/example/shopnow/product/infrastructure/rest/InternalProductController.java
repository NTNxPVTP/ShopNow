package com.example.shopnow.product.infrastructure.rest;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.shopnow.product.api.ProductApi;
import com.example.shopnow.product.api.dto.OrderLineRequest;
import com.example.shopnow.product.api.dto.ProductInfoForOrder;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/internal/products")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductApi productApi;

    @PostMapping("/decrease")
    public ResponseEntity<List<ProductInfoForOrder>> decreaseProducts(@RequestBody List<OrderLineRequest> lines) {
        return ResponseEntity.ok(productApi.decreaseProducts(lines));
    }

    @PostMapping("/info")
    public ResponseEntity<List<ProductInfoForOrder>> getProductsInfo(@RequestBody List<OrderLineRequest> lines) {
        return ResponseEntity.ok(productApi.getProductsInfo(lines));
    }
}
