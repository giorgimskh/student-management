package com.example.student_management.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedResponse<T>(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last,
        List<T> content
) {
    public static <T> PagedResponse<T> from(Page<T> page){
        return new PagedResponse<>(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.getContent()
        );
    }
}
