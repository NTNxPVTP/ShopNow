package com.example.shopnow.product;

import com.example.shopnow.product.api.dto.ProductInfoForOrder;
import com.example.shopnow.product.application.dto.CreateProductRequest;
import com.example.shopnow.product.application.dto.ProductDetailResponse;
import com.example.shopnow.product.application.dto.UpdateProductRequest;
import com.example.shopnow.product.domain.models.Product;
import com.example.shopnow.product.domain.models.ProductStatus;
import com.example.shopnow.product.domain.models.Shop;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-25T13:47:18+0700",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class ProductMapperImpl implements ProductMapper {

    @Override
    public List<ProductDetailResponse> toDtoList(List<Product> entities) {
        if ( entities == null ) {
            return null;
        }

        List<ProductDetailResponse> list = new ArrayList<ProductDetailResponse>( entities.size() );
        for ( Product product : entities ) {
            list.add( toDto( product ) );
        }

        return list;
    }

    @Override
    public ProductDetailResponse toDto(Product product) {
        if ( product == null ) {
            return null;
        }

        UUID shopId = null;
        UUID id = null;
        String name = null;
        String pictureUrl = null;
        Integer quantity = null;
        BigDecimal price = null;
        ProductStatus status = null;

        shopId = productShopId( product );
        id = product.getId();
        name = product.getName();
        pictureUrl = product.getPictureUrl();
        quantity = product.getQuantity();
        price = product.getPrice();
        status = product.getStatus();

        Set<UUID> categoryIds = mapCategoryIds(product.getCategories());

        ProductDetailResponse productDetailResponse = new ProductDetailResponse( id, name, pictureUrl, quantity, price, status, shopId, categoryIds );

        return productDetailResponse;
    }

    @Override
    public Product fromCreateRequestToProduct(CreateProductRequest request) {
        if ( request == null ) {
            return null;
        }

        Product.ProductBuilder product = Product.builder();

        product.name( request.name() );
        product.pictureUrl( request.pictureUrl() );
        product.price( request.price() );
        product.quantity( request.quantity() );

        return product.build();
    }

    @Override
    public void updateProductFromUpdateRequest(UpdateProductRequest request, Product product) {
        if ( request == null ) {
            return;
        }

        if ( request.name() != null ) {
            product.setName( request.name() );
        }
        if ( request.pictureUrl() != null ) {
            product.setPictureUrl( request.pictureUrl() );
        }
        if ( request.price() != null ) {
            product.setPrice( request.price() );
        }
        if ( request.quantity() != null ) {
            product.setQuantity( request.quantity() );
        }
    }

    @Override
    public ProductInfoForOrder toProductInfoForOrder(Product product) {
        if ( product == null ) {
            return null;
        }

        UUID shopId = null;
        UUID shopOwnerId = null;
        UUID id = null;
        BigDecimal price = null;
        String name = null;
        Integer quantity = null;

        shopId = productShopId( product );
        shopOwnerId = productShopOwnerId( product );
        id = product.getId();
        price = product.getPrice();
        name = product.getName();
        quantity = product.getQuantity();

        ProductInfoForOrder productInfoForOrder = new ProductInfoForOrder( id, price, name, quantity, shopId, shopOwnerId );

        return productInfoForOrder;
    }

    @Override
    public List<ProductInfoForOrder> toProductInfoForOrders(List<Product> products) {
        if ( products == null ) {
            return null;
        }

        List<ProductInfoForOrder> list = new ArrayList<ProductInfoForOrder>( products.size() );
        for ( Product product : products ) {
            list.add( toProductInfoForOrder( product ) );
        }

        return list;
    }

    private UUID productShopId(Product product) {
        Shop shop = product.getShop();
        if ( shop == null ) {
            return null;
        }
        return shop.getId();
    }

    private UUID productShopOwnerId(Product product) {
        Shop shop = product.getShop();
        if ( shop == null ) {
            return null;
        }
        return shop.getOwnerId();
    }
}
