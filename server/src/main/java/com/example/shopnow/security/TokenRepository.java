package com.example.shopnow.security;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.shopnow.security.models.Token;

public interface TokenRepository extends JpaRepository<Token,UUID> {
    @Query(value = """
                        Select t from Token t
                                where t.user.id = :userId
                                and t.expired = false and t.revoked=false """)
        List<Token> findAllValidTokenByUser(UUID userId);
        @EntityGraph(attributePaths = { "user" })
        List<Token> findAllTokensByUserEmail(String email);
        Optional<Token> findByToken(String token);
} 