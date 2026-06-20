package com.example.shopnow.user.application.usecases;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.shopnow.user.UserRepository;
import com.example.shopnow.user.api.AuthenticatedUser;
import com.example.shopnow.user.models.User;

import lombok.RequiredArgsConstructor;

/**
 * Use case (driving port, intra-module) that provisions a user for the OAuth
 * login flow.
 *
 * <p>This is the one place in the {@code user} module where real orchestration
 * exists: a get-or-create that looks the user up by email and, when absent,
 * creates a CUSTOMER with a random placeholder password and persists it. The
 * lookup-only {@code findByEmail} path has no invariant or orchestration and is
 * therefore left inline in {@code UserServiceImpl} rather than wrapped here.
 *
 * <p>Behaviour is preserved exactly from the previous inline implementation:
 * the role-validity invariant (OAuth users are always {@link com.example.shopnow.user.models.Role#CUSTOMER})
 * is enforced by the {@link User#createOAuthCustomer} domain factory, and the
 * placeholder password remains a random {@link UUID} string.
 *
 * <p>This use case stays internal to the {@code user} module; the published
 * {@link com.example.shopnow.user.api.UserApi} continues to be the only
 * cross-module surface and delegates here.
 */
@Service
@RequiredArgsConstructor
public class ProvisionOAuthUserUseCase {

    private final UserRepository repository; // driven port (intra-module)

    /**
     * Get-or-create: returns the existing user when {@code email} is already
     * registered, otherwise creates and persists a new CUSTOMER user with a
     * random placeholder password.
     *
     * @param email the user's email (used as the username)
     * @param name  the user's display name
     * @return the existing or newly created principal for {@code email}
     */
    public AuthenticatedUser execute(String email, String name) {
        return repository.findByEmail(email).orElseGet(() -> {
            User newUser = User.createOAuthCustomer(
                    email, name, UUID.randomUUID().toString()); // random placeholder password
            return repository.save(newUser);
        });
    }
}
