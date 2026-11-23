package com.swordshield.next.identity;

import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.util.Base64;

import org.springframework.test.context.DynamicPropertyRegistry;

final class PortalTestSigningKey {

    private static final String LOCATION = generate();

    /** 使用仅本次测试进程持有的外置签名密钥。 */
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("identity.portal.private-key", () -> LOCATION);
    }

    /** 生成临时 PKCS#8 文件，让测试经过生产使用的密钥加载路径。 */
    private static String generate() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            var pem = Files.createTempFile("identity-test-signing-", ".pem");
            pem.toFile().deleteOnExit();
            String key = Base64.getMimeEncoder(64, new byte[]{'\n'})
                    .encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
            Files.writeString(pem, "-----BEGIN PRIVATE KEY-----\n" + key + "\n-----END PRIVATE KEY-----\n");
            return pem.toUri().toString();
        } catch (GeneralSecurityException | IOException ex) {
            throw new IllegalStateException("创建测试签名密钥失败", ex);
        }
    }
}
