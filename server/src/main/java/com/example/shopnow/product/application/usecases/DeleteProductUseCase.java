package com.example.shopnow.product.application.usecases;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.product.domain.repository.ProductRepository;
import com.example.shopnow.user.api.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

/**
 * Use case (driving port) that soft-deletes a product owned by the
 * authenticated shop owner.
 *
 * <p>Exposes a single write execution entry point,
 * {@link #execute(UUID, AuthenticatedUser)}. The soft-delete is an atomic,
 * owner-scoped {@code UPDATE} through the {@link ProductRepository} port; when
 * it affects 0 rows (not found / not owned) {@link ErrorCode#PRODUCT_NOT_FOUND}
 * is thrown. This preserves the exact behavior previously implemented in
 * {@code ProductServiceImpl.deleteProduct}.
 */
@Service
@RequiredArgsConstructor
public class DeleteProductUseCase {

    private final ProductRepository productRepository;

    @Transactional(rollbackFor = Exception.class)
    public String execute(UUID id, AuthenticatedUser owner) {
        UUID shopOwnerId = owner.getId();
        if (shopOwnerId == null) {
            throw new DomainException(ErrorCode.USER_NOT_FOUND);
        }
        int check = productRepository.softDelete(id, shopOwnerId);
        if (check == 0) {
            throw new DomainException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return "Deleted product with id: " + id;
    }
}
