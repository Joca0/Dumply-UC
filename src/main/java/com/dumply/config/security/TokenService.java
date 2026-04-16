package com.dumply.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.dumply.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    @Value( "${jwt.api.secret}")
    private String secret;

    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            String token = JWT.create()
                    .withIssuer("auth-login")
                    .withSubject(user.getEmail())
                    .withClaim("companyId", user.getCompany().getId().toString())
                    .withClaim("role", user.getRole().name())
                    .withExpiresAt(this.generateExpirationDate())
                    .sign(algorithm);
            return token;
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao autenticar", exception);
        }
    }

    public Instant generateExpirationDate() {
        return LocalDateTime.now().plusHours(8).toInstant(ZoneOffset.ofHours(-3));
    }

    public DecodedJWT validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("auth-login")
                    .build()
                    .verify(token);



        } catch (JWTVerificationException exception) {
            return null;
        }
    }


}
