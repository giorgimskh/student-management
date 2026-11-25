package com.example.student_management.storage;

import com.example.student_management.domain.Student;
import com.example.student_management.domain.StudentPhoto;
import com.example.student_management.repository.StudentPhotoRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Keeps photo bytes in the {@code student_photos} table. */
@Component
public class DatabasePhotoStorage implements PhotoStorage {
    private final StudentPhotoRepository studentPhotoRepository;

    public DatabasePhotoStorage(StudentPhotoRepository studentPhotoRepository) {
        this.studentPhotoRepository = studentPhotoRepository;
    }

    @Override
    public StudentPhoto save(Student student, String originalFilename, String contentType, byte[] data, Instant uploadedAt) {
        StudentPhoto photo = studentPhotoRepository.findByStudentId(student.getId())
                .orElseGet(() -> {
                    StudentPhoto newPhoto = new StudentPhoto();
                    newPhoto.setStudent(student);
                    return newPhoto;
                });

        photo.setOriginalFilename(originalFilename);
        photo.setContentType(contentType);
        photo.setData(data);
        photo.setSizeBytes(data.length);
        photo.setUploadedAt(uploadedAt);
        return studentPhotoRepository.save(photo);
    }

    @Override
    public Optional<StudentPhoto> findByStudentId(UUID studentId) {
        return studentPhotoRepository.findByStudentId(studentId);
    }

    @Override
    public void deleteByStudentId(UUID studentId) {
        studentPhotoRepository.findByStudentId(studentId)
                .ifPresent(studentPhotoRepository::delete);
    }
}
