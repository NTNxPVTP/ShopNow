package com.example.shopnow.product;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.example.shopnow.product.api.dto.ProductInfoForOrder;
import com.example.shopnow.product.models.Category;
import com.example.shopnow.product.models.Product;
import com.example.shopnow.product.rest.dto.CreateProductRequest;
import com.example.shopnow.product.rest.dto.ProductDetailResponse;
import com.example.shopnow.product.rest.dto.UpdateProductRequest;
import com.example.shopnow.shared.GenericMapper;

@Mapper(componentModel = "spring")
interface ProductMapper extends GenericMapper<Product, ProductDetailResponse> {

    @Mapping(target = "shopId", source = "shop.id")
    @Mapping(target = "categoryIds", expression = "java(mapCategoryIds(product.getCategories()))")
    ProductDetailResponse toDto(Product product);

    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product fromCreateRequestToProduct(CreateProductRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateProductFromUpdateRequest(UpdateProductRequest request, @MappingTarget Product product);

    @Mapping(target = "shopId", source = "shop.id")
    @Mapping(target ="shopOwnerId", source = "shop.ownerId")
    ProductInfoForOrder toProductInfoForOrder(Product product);

    List<ProductInfoForOrder> toProductInfoForOrders(List<Product> products);

    default Set<UUID> mapCategoryIds(Set<Category> categories) {
        if (categories == null) {
            return Set.of();
        }
        return categories.stream().map(Category::getId).collect(java.util.stream.Collectors.toSet());
    }

}
