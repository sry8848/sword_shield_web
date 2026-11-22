package com.swordshield.next.identity;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PasswordHashCommandTest {

    @Test
    void encodedPasswordIsCompatibleWithApplicationEncoder() {
        char[] input = "LocalFixture-Only!2026".toCharArray();
        String hash = PasswordHashCommand.encode(input);

        Argon2PasswordEncoder verifier = IdentityPasswordConfiguration.identityPasswordEncoder();
        assertThat(hash).startsWith("$argon2id$v=19$m=19456,t=2,p=1$");
        assertThat(verifier.matches("LocalFixture-Only!2026", hash)).isTrue();
        assertThat(verifier.matches("wrong-password", hash)).isFalse();
        assertThat(input).containsOnly('\0');
    }

    @Test
    void emptyPasswordIsRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> PasswordHashCommand.encode(new char[0]));
    }
}
