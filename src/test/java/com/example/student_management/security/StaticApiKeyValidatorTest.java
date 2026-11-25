package com.example.student_management.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StaticApiKeyValidatorTest {
    private final ApiKeyValidator validator = new StaticApiKeyValidator("secret");

    @Test
    void acceptsOnlyTheExactKey() {
        assertThat(validator.isValid("secret")).isTrue();
        assertThat(validator.isValid("Secret")).isFalse();
        assertThat(validator.isValid("secret ")).isFalse();
        assertThat(validator.isValid("")).isFalse();
    }

    @Test
    void rejectsMissingKey() {
        assertThat(validator.isValid(null)).isFalse();
    }

    @Test
    void requiresAConfiguredKey() {
        assertThatThrownBy(() -> new StaticApiKeyValidator(null))
                .isInstanceOf(NullPointerException.class);
    }
}
