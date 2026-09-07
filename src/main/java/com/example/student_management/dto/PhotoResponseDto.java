package com.example.student_management.dto;

import jakarta.annotation.Resource;

public record PhotoResponseDto(
        Resource getResource,
        String getContentType,
        String getFilename
) {}
