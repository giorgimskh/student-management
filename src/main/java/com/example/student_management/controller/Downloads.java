package com.example.student_management.controller;

import com.example.student_management.export.ExportFormatFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/** Builds a file download response for an export. */
final class Downloads {
    private Downloads() {
    }

    static ResponseEntity<String> attachment(String fileName, ExportFormatFactory format, String body) {
        return ResponseEntity.ok()
                .contentType(format.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .body(body);
    }
}
