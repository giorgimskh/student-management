package com.example.student_management.dto;

import org.springframework.core.io.Resource;

public record PhotoResponseDto(
        Resource resource,
        String contentType,
        String filename
) {}
