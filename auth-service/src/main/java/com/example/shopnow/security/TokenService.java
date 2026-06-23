package com.example.shopnow.security;

import org.springframework.stereotype.Service;
import com.example.shopnow.security.models.Token;
import com.example.shopnow.security.models.TokenType;
import com.example.shopnow.user.api.AuthenticatedUser;
import com.example.shopnow.user.api.UserApi;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final TokenRepository tokenRepository;
    private final UserApi userApi;

    public void revokeAllUserTokens(AuthenticatedUser user){
        var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
        if(validUserTokens.isEmpty()){
            return;
        }
        validUserTokens.forEach(token->{
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

    public void revokeAllUserTokens(String email){
        userApi.findByEmail(email).ifPresent(this::revokeAllUserTokens);
    }

    public void saveUserTokens(AuthenticatedUser user, String jwtToken, String refreshToken){
        saveAccessToken(user,jwtToken);
        saveRefreshToken(user,refreshToken);
    }

    private void saveRefreshToken(AuthenticatedUser user, String refreshToken) {
        saveUserToken(user,refreshToken,TokenType.REFRESH_TOKEN);
    }

    private void saveAccessToken(AuthenticatedUser user, String jwtToken) {
        saveUserToken(user, jwtToken, TokenType.ACCESS_TOKEN);
    }

    private void saveUserToken(AuthenticatedUser user, String jwtToken, TokenType tokenType) {
        var token = Token.builder()
                    .userId(user.getId())
                    .token(jwtToken)
                    .tokenType(tokenType)
                    .expired(false)
                    .revoked(false)
                    .build();
        tokenRepository.save(token);
    }
}
