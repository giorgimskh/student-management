package com.example.student_management.service;

import com.example.student_management.domain.StudentPhoto;
import com.example.student_management.dto.PhotoResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** Upload, download and removal of a student's profile photo. */
public interface StudentPhotoService {
    /** Validates the upload and stores it, replacing any existing photo. */
    StudentPhoto storePhoto(UUID studentId, MultipartFile file);

    PhotoResponseDto getPhoto(UUID studentId);

    /** Deletes the photo; 404 if the student or the photo does not exist. */
    void deletePhoto(UUID studentId);

    /** Deletes the photo if there is one; never fails because it is missing. */
    void deletePhotoIfExists(UUID studentId);
}
