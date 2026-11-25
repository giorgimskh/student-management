package com.example.student_management.service;

import com.example.student_management.domain.Student;
import com.example.student_management.domain.StudentPhoto;
import com.example.student_management.exceptions.InvalidPhotoException;
import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.repository.StudentRepository;
import com.example.student_management.service.impl.StudentPhotoServiceImpl;
import com.example.student_management.storage.PhotoStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentPhotoServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private PhotoStorage photoStorage;

    @Mock
    private StudentRepository studentRepository;

    private StudentPhotoService service;

    @BeforeEach
    void setUp() {
        service = new StudentPhotoServiceImpl(photoStorage, studentRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void storePhoto_validatesThenHandsBytesToStorage() {
        UUID id = UUID.randomUUID();
        Student student = Student.builder().id(id).name("Nino").email("nino@example.com").build();
        byte[] bytes = {1, 2, 3};
        when(studentRepository.findById(id)).thenReturn(Optional.of(student));

        service.storePhoto(id, new MockMultipartFile("file", "me.png", "image/png", bytes));

        verify(photoStorage).save(student, "me.png", "image/png", bytes, NOW);
    }

    @Test
    void storePhoto_rejectsNonImagesWithoutTouchingStorage() {
        MockMultipartFile text = new MockMultipartFile("file", "a.txt", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> service.storePhoto(UUID.randomUUID(), text))
                .isInstanceOf(InvalidPhotoException.class);
        verifyNoInteractions(photoStorage);
    }

    @Test
    void deletePhoto_returns404WhenThereIsNoPhoto() {
        UUID id = UUID.randomUUID();
        when(studentRepository.existsById(id)).thenReturn(true);
        when(photoStorage.findByStudentId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletePhoto(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(photoStorage, never()).deleteByStudentId(any());
    }

    @Test
    void deletePhoto_removesExistingPhoto() {
        UUID id = UUID.randomUUID();
        when(studentRepository.existsById(id)).thenReturn(true);
        when(photoStorage.findByStudentId(id)).thenReturn(Optional.of(new StudentPhoto()));

        service.deletePhoto(id);

        verify(photoStorage).deleteByStudentId(id);
    }
}
