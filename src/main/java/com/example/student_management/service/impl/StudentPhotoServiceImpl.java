package com.example.student_management.service.impl;

import com.example.student_management.domain.Student;
import com.example.student_management.domain.StudentPhoto;
import com.example.student_management.dto.PhotoResponseDto;
import com.example.student_management.exceptions.InvalidPhotoException;
import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.repository.StudentRepository;
import com.example.student_management.service.StudentPhotoService;
import com.example.student_management.storage.PhotoStorage;
import jakarta.transaction.Transactional;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class StudentPhotoServiceImpl implements StudentPhotoService {
    private final PhotoStorage photoStorage;
    private final StudentRepository studentRepository;
    private final Clock clock;

    public StudentPhotoServiceImpl(PhotoStorage photoStorage, StudentRepository studentRepository, Clock clock) {
        this.photoStorage = photoStorage;
        this.studentRepository = studentRepository;
        this.clock = clock;
    }

    @Override
    public StudentPhoto storePhoto(UUID studentId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidPhotoException("Photo file must not be empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new InvalidPhotoException("Photo must be an image (got content type: " + contentType + ")");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> studentNotFound(studentId));

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload photo for student id: " + studentId, e);
        }

        return photoStorage.save(student, file.getOriginalFilename(), contentType, data, Instant.now(clock));
    }

    @Override
    public PhotoResponseDto getPhoto(UUID studentId) {
        StudentPhoto studentPhoto = findExistingPhoto(studentId);

        return new PhotoResponseDto(
                new ByteArrayResource(studentPhoto.getData()),
                studentPhoto.getContentType(),
                studentPhoto.getOriginalFilename()
        );
    }

    @Override
    public void deletePhoto(UUID studentId) {
        findExistingPhoto(studentId); // 404 if the student or photo is missing
        photoStorage.deleteByStudentId(studentId);
    }

    @Override
    public void deletePhotoIfExists(UUID studentId) {
        photoStorage.deleteByStudentId(studentId);
    }

    private StudentPhoto findExistingPhoto(UUID studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw studentNotFound(studentId);
        }

        return photoStorage.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo not found for student id: " + studentId));
    }

    private static ResourceNotFoundException studentNotFound(UUID studentId) {
        return new ResourceNotFoundException("Student not found with id: " + studentId);
    }
}
