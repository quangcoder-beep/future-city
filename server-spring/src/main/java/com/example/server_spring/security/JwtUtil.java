package com.example.server_spring.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {

    private final String SIGNER_KEY = "Qu56GJ2MNbI8U6HvMjGS249Bzz6PsLwE";

    /** Táº¡o Access Token JWT, háº¿t háº¡n sau 15 phĂºt. */
    public String generateToken(int userId) {
        return buildToken(userId, 900, "access"); // 900s = 15 phĂºt
    }

    /**
     * Táº¡o Refresh Token JWT, háº¿t háº¡n sau 30 ngĂ y. KhĂ´ng cáº§n DB vĂ¬ lĂ  JWT tá»± xĂ¡c
     * minh.
     */
    public String generateRefreshToken(int userId) {
        return buildToken(userId, 30L * 24 * 3600, "refresh"); // 30 ngĂ y
    }

    private String buildToken(int userId, long expireSeconds, String type) {
        try {
            JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(userId))
                    .claim("userId", userId)
                    .claim("type", type)
                    .issuer("com.futurecity.server")
                    .issueTime(new Date())
                    .expirationTime(new Date(Instant.now().plusSeconds(expireSeconds).toEpochMilli()))
                    .build();

            Payload payload = new Payload(claims.toJSONObject());
            JWSObject jwsObject = new JWSObject(header, payload);
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error generating token", e);
        }
    }

    /**
     * XĂ¡c minh Access Token JWT (type="access").
     * 
     * @return userId náº¿u há»£p lá»‡, -1 náº¿u sai/háº¿t háº¡n/sai loáº¡i.
     */
    public int verifyToken(String token) {
        return verifyTokenOfType(token, "access");
    }

    /**
     * XĂ¡c minh Refresh Token JWT (type="refresh").
     * 
     * @return userId náº¿u há»£p lá»‡, -1 náº¿u sai/háº¿t háº¡n/sai loáº¡i.
     */
    public int verifyRefreshToken(String token) {
        return verifyTokenOfType(token, "refresh");
    }

    private int verifyTokenOfType(String token, String expectedType) {
        try {
            JWSObject jwsObject = JWSObject.parse(token);

            com.nimbusds.jose.crypto.MACVerifier verifier = new com.nimbusds.jose.crypto.MACVerifier(
                    SIGNER_KEY.getBytes());
            if (!jwsObject.verify(verifier))
                return -1;

            JWTClaimsSet claims = JWTClaimsSet.parse(jwsObject.getPayload().toJSONObject());

            // Kiá»ƒm tra háº¿t háº¡n
            if (claims.getExpirationTime() != null
                    && claims.getExpirationTime().before(new Date())) {
                return -1;
            }

            // Kiá»ƒm tra Ä‘Ăºng loáº¡i token
            String type = (String) claims.getClaim("type");
            if (!expectedType.equals(type))
                return -1;

            return claims.getIntegerClaim("userId");
        } catch (Exception e) {
            return -1;
        }
    }
}
