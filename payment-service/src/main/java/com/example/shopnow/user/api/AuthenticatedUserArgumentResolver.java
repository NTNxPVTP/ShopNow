package com.example.shopnow.user.api;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

@Component
public class AuthenticatedUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(AuthUser.class) != null
                && parameter.getParameterType().equals(AuthenticatedUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        String userIdStr = webRequest.getHeader("X-Auth-User-Id");
        if (userIdStr == null || userIdStr.isEmpty()) {
            throw new RuntimeException("Unauthorized: Missing X-Auth-User-Id header");
        }
        UUID userId = UUID.fromString(userIdStr);

        return new AuthenticatedUser() {
            @Override
            public UUID getId() {
                return userId;
            }

            @Override
            public String getEmail() {
                return "gateway-user@example.com";
            }

            @Override
            public String getRole() {
                return "USER";
            }
        };
    }
}
