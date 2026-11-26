package com.swordshield.next.identity;

import java.util.UUID;
import java.util.stream.IntStream;

import com.swordshield.next.common.UuidV7Ids;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UuidV7IdsTest {
    @Test
    void generatesCanonicalVersion7IdsWithoutDuplicatesAcrossThreads() {
        var ids = IntStream.range(0, 10_000).parallel().mapToObj(index -> UuidV7Ids.next()).toList();

        assertThat(ids).doesNotHaveDuplicates().allSatisfy(id -> {
            assertThat(id).hasSize(36).matches("[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
            UUID uuid = UUID.fromString(id);
            assertThat(uuid.version()).isEqualTo(7);
            assertThat(uuid.variant()).isEqualTo(2);
        });
    }
}
