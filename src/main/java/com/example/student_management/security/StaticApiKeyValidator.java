package com.example.student_management.security;

import java.util.Objects;

/** Accepts exactly one configured key. */
public class StaticApiKeyValidator implements ApiKeyValidator {
    private final String expectedApiKey;

    public StaticApiKeyValidator(String expectedApiKey) {
        this.expectedApiKey = Objects.requireNonNull(expectedApiKey, "expectedApiKey");
    }

    @Override
    public boolean isValid(String apiKey) {
        return expectedApiKey.equals(apiKey);
    }
}
