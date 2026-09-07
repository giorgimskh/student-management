package com.example.student_management.controller;

import com.example.student_management.domain.StudentPhoto;
import com.example.student_management.dto.PhotoResponseDto;
import com.example.student_management.service.StudentPhotoService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/students/{studentId}/photo")
public class StudentPhotoController {
    private final StudentPhotoService studentPhotoService;

    public StudentPhotoController(StudentPhotoService studentPhotoService) {
        this.studentPhotoService = studentPhotoService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentPhoto> uploadPhoto(@PathVariable UUID studentId, @RequestParam("file") MultipartFile file){
        studentPhotoService.storePhoto(studentId,file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deletePhoto(@PathVariable UUID studentId) {
        studentPhotoService.deletePhoto(studentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Resource> downloadPhoto(@PathVariable UUID studentId) {
        PhotoResponseDto photoDto = studentPhotoService.getPhoto(studentId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photoDto.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + photoDto.getFilename() + "\"")
                .body(photoDto.getResource());
    }
}
