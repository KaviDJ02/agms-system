package com.agms.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.HexFormat;

@Slf4j
@Component
public class JwtUtil {

    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret}") String hexSecret) {
        byte[] keyBytes = HexFormat.of().parseHex(hexSecret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Validates the token and returns the subject (username/userId).
     * Throws a JwtException if the token is expired, malformed, or has an invalid signature.
     */
    public String validateAndGetSubject(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }
}
