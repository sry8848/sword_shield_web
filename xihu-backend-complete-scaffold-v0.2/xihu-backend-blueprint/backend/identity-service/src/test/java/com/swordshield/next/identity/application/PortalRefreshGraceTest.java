package com.swordshield.next.identity.application;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PortalRefreshGraceTest {

    @Test
    void includesExactlyTenSeconds() {
        Instant consumedAt = Instant.parse("2026-11-25T09:00:00.123Z");
        assertThat(PortalAuthentication.withinRefreshGrace(consumedAt, consumedAt.plusMillis(9_999))).isTrue();
        assertThat(PortalAuthentication.withinRefreshGrace(consumedAt, consumedAt.plusSeconds(10))).isTrue();
        assertThat(PortalAuthentication.withinRefreshGrace(consumedAt, consumedAt.plusMillis(10_001))).isFalse();
    }
}
