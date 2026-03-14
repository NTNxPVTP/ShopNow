package com.example.shopnow.product;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final  ProductService productService;
    
    @GetMapping("/{id}")
    public ResponseEntity<Product> viewDetailsOfProduct(@PathVariable Integer id) {
        Product product = productService.viewDetailsOfProduct(id);
        return ResponseEntity.ok(product);
    }

    // @PostMapping
    // public ResponseEntity<ProductDetail> createProduct(@RequestBody CreateProductRequest request){
    //     ProductDetail detail = productService.createProduct(request);
    //     return ResponseEntity.ok(detail);
    // }
}
