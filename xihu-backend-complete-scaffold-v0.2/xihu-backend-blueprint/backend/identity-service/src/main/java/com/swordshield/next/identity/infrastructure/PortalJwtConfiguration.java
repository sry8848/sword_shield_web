package com.swordshield.next.identity.infrastructure;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.swordshield.next.identity.application.PortalAuthentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration(proxyBeanMethods = false)
class PortalJwtConfiguration {

    private static final String AUDIENCE = "portal-api";

    /** 从外置 PKCS#8 PEM 读取签名私钥，并据此计算验签公钥和稳定的 kid。 */
    @Bean
    RSAKey portalSigningKey(@Value("${identity.portal.private-key}") Resource pem) throws Exception {
        // 1. 读取私钥中的公钥参数，避免部署时误配另一把公钥。
        try (var input = pem.getInputStream()) {
            var privateKey = (RSAPrivateCrtKey) RsaKeyConverters.pkcs8().convert(input);
            var spec = new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent());
            var publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
            return new RSAKey.Builder(publicKey).privateKey(privateKey).keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256).keyIDFromThumbprint().build();
        }
    }

    /** 使用同一把 RSA 密钥签发 JWT，并在头部带上 JWKS 中的 kid。 */
    @Bean
    JwtEncoder portalJwtEncoder(RSAKey key) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }

    /** 按 Portal 的签发者、受众和应用类型签发 600 秒 Access Token。 */
    @Bean
    PortalAuthentication.AccessTokens portalAccessTokens(JwtEncoder encoder, RSAKey key,
            @Value("${identity.portal.issuer}") String issuer) {
        return (account, session) -> {
            // 1. 构建身份和会话 Claim。后台角色不授予 Portal 凭证后台权限。
            var claims = JwtClaimsSet.builder()
                    .issuer(issuer)
                    .subject(account.id())
                    .audience(List.of(AUDIENCE))
                    .issuedAt(session.createdAt())
                    .expiresAt(session.createdAt().plusSeconds(PortalAuthentication.ACCESS_SECONDS))
                    .id(UUID.randomUUID().toString())
                    .claim("sid", session.id())
                    .claim("client_type", PortalAuthentication.CLIENT)
                    .claim("credential_version", account.credentialVersion())
                    .claim("permission_version", account.permissionVersion())
                    .claim("roles", List.of())
                    .build();
            var header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(key.getKeyID()).build();
            return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        };
    }

    /** Identity 用本地公钥验签，并区分已过期凭证和其他无效凭证。 */
    @Bean
    JwtDecoder portalJwtDecoder(RSAKey key, @Value("${identity.portal.issuer}") String issuer) throws Exception {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256).build();
        decoder.setJwtValidator(portalClaims(issuer));
        return decoder;
    }

    /** 校验 Portal 身份字段和时间窗口，过期只适用于其他约束已经满足的 JWT。 */
    private static OAuth2TokenValidator<Jwt> portalClaims(String issuer) {
        var timestamps = new JwtTimestampValidator(Duration.ZERO);
        return jwt -> {
            // 1. 确认凭证由本环境签发且面向 Portal API。
            if (!issuer.equals(jwt.getClaimAsString("iss"))
                    || jwt.getAudience() == null || !jwt.getAudience().contains(AUDIENCE)
                    || !PortalAuthentication.CLIENT.equals(jwt.getClaimAsString("client_type"))
                    || jwt.getSubject() == null || jwt.getExpiresAt() == null) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
            }

            // 2. 明确区分已过期凭证，供前端下一批实现自动刷新。
            if (!jwt.getExpiresAt().isAfter(Instant.now())) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("ACCESS_TOKEN_EXPIRED"));
            }
            return timestamps.validate(jwt);
        };
    }
}
