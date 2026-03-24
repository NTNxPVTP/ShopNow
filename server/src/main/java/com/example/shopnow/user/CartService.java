package com.example.shopnow.user;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.example.shopnow.user.models.Cart;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartService {

    final private CartRepository cartRepository;
    public String updateProductQuantity(String name,Integer num){
        Cart cart = cartRepository.findByName(name);
        return "update successfully";
    }
}
