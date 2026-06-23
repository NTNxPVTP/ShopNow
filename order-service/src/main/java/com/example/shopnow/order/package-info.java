@org.springframework.modulith.ApplicationModule(
    displayName = "Order Management",
    allowedDependencies = {"product :: product-api", "product :: product-api-dto", "user :: user-api", "user :: user-api-dto", "shared", "exception"}
)
package com.example.shopnow.order;
