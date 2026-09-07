package com.example.student_management.controller;

import com.example.student_management.domain.StudentPhoto;
import com.example.student_management.service.StudentPhotoService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/students/studentId/photo")
public class StudentPhotoController {
    private final StudentPhotoService studentPhotoService;

    public StudentPhotoController(StudentPhotoService studentPhotoService) {
        this.studentPhotoService = studentPhotoService;
    }

    @PostMapping
    public ResponseEntity<StudentPhoto> uploadPhoto(@RequestPart("studentId") UUID studentId, @RequestPart("file") MultipartFile file){
        studentPhotoService.storePhoto(studentId,file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deletePhoto(@PathVariable UUID studentId) {
        studentPhotoService.deletePhoto(studentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<StudentPhoto> downloadPhoto(@PathVariable UUID studentId){
        return ResponseEntity.ok(studentPhotoService.getPhoto(studentId));
    }
}
