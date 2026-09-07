package com.example.student_management.service;

import com.example.student_management.domain.Student;
import com.example.student_management.domain.StudentPhoto;
import com.example.student_management.dto.PhotoResponseDto;
import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.repository.StudentPhotoRepository;
import com.example.student_management.repository.StudentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class StudentPhotoService {
    private final StudentPhotoRepository studentPhotoRepository;
    private final StudentRepository studentRepository;
    private final Clock clock;

    public StudentPhotoService(StudentPhotoRepository studentPhotoRepository, StudentRepository studentRepository, Clock clock) {
        this.studentPhotoRepository = studentPhotoRepository;
        this.studentRepository = studentRepository;
        this.clock = clock;
    }

    public StudentPhoto storePhoto(UUID studentId, MultipartFile file){
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        StudentPhoto photo = studentPhotoRepository.findByStudentId(studentId)
                .orElseGet(() -> {
                    StudentPhoto newPhoto = new StudentPhoto();
                    newPhoto.setStudent(student);
                    return newPhoto;
                });

        try {
            photo.setOriginalFilename(file.getOriginalFilename());
            photo.setContentType(file.getContentType());
            photo.setData(file.getBytes());
            photo.setSizeBytes(file.getSize());
            photo.setUploadedAt(Instant.now(clock));
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload Photo for studentId" + studentId,e);
        }

        return studentPhotoRepository.save(photo);
    }

    public PhotoResponseDto getPhoto(UUID studentId){
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        StudentPhoto studentPhoto=studentPhotoRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo not found with id : " + studentId));

        return studentPhoto;
    }

    public void deletePhoto(UUID studentId){
       if(!studentRepository.existsById(studentId)){
           throw new ResourceNotFoundException("Student does not exist with id : "+studentId);
       }

        StudentPhoto studentPhoto=studentPhotoRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student with id "+studentId + " does not have Photo"));

        studentPhotoRepository.delete(studentPhoto);
    }

    public void deletePhotoIfExists(UUID studentId) {
        studentPhotoRepository.findByStudentId(studentId)
                .ifPresent(studentPhotoRepository::delete);
    }
}
