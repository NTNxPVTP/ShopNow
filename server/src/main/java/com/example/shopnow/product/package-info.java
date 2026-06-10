@org.springframework.modulith.ApplicationModule(
    displayName = "Product Catalog",
    allowedDependencies = {"user :: user-api", "user :: user-api-dto", "shared", "exception"}
)
package com.example.shopnow.product;
