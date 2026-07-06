package io.rosecloud.starter.sequence;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link LocalSequenceGenerator} produces monotonic per-key sequences
 * starting at 1, and that distinct keys are independent.
 */
class LocalSequenceGeneratorTest {

    @Test
    void monotonicPerKey() {
        LocalSequenceGenerator generator = new LocalSequenceGenerator();

        assertThat(generator.next("order")).isEqualTo(1L);
        assertThat(generator.next("order")).isEqualTo(2L);
        assertThat(generator.next("order")).isEqualTo(3L);
    }

    @Test
    void distinctKeysAreIndependent() {
        LocalSequenceGenerator generator = new LocalSequenceGenerator();

        assertThat(generator.next("order")).isEqualTo(1L);
        assertThat(generator.next("invoice")).isEqualTo(1L);
        assertThat(generator.next("order")).isEqualTo(2L);
        assertThat(generator.next("invoice")).isEqualTo(2L);
    }
}
