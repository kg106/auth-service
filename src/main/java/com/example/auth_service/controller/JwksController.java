package com.example.auth_service.controller;

import com.example.auth_service.security.RsaKeyManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.Map;
import java.util.List;

@RestController
public class JwksController {

    private final RsaKeyManager rsaKeyManager;

    public JwksController(RsaKeyManager rsaKeyManager) {
        this.rsaKeyManager = rsaKeyManager;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> getJwks() {
        String n = Base64.getUrlEncoder().withoutPadding().encodeToString(
                rsaKeyManager.getPublicKey().getModulus().toByteArray()
        );
        String e = Base64.getUrlEncoder().withoutPadding().encodeToString(
                rsaKeyManager.getPublicKey().getPublicExponent().toByteArray()
        );

        Map<String, Object> keyDetails = Map.of(
                "kty", "RSA",
                "use", "sig",
                "alg", "RS256",
                "kid", rsaKeyManager.getKeyId(),
                "n", n,
                "e", e
        );

        return Map.of("keys", List.of(keyDetails));
    }
}
