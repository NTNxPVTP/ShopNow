package com.example.shopnow.demo;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.shopnow.user.models.User;

@RestController
@RequestMapping("/demo")
public class DemoController {
    @GetMapping("/hello")
    public String helloWorld(@AuthenticationPrincipal User user){
        return "Hello world " + user.getEmail() + " " + user.getRole();
    }
}
