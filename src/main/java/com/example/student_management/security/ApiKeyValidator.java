package com.example.student_management.security;

/** Decides whether an API key sent by a client is allowed to call the API. */
@FunctionalInterface
public interface ApiKeyValidator {
    /** @param apiKey the raw header value; may be {@code null} if the header was missing */
    boolean isValid(String apiKey);
}
