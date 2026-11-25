package com.example.student_management.service;

import com.example.student_management.domain.Student;
import com.example.student_management.domain.StudentPhoto;
import com.example.student_management.dto.PhotoResponseDto;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
    void storePhoto_rejectsMissingOrEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.storePhoto(UUID.randomUUID(), null))
                .isInstanceOf(InvalidPhotoException.class);
        assertThatThrownBy(() -> service.storePhoto(UUID.randomUUID(), empty))
                .isInstanceOf(InvalidPhotoException.class);
        verifyNoInteractions(photoStorage, studentRepository);
    }

    @Test
    void storePhoto_rejectsMissingContentType() {
        MockMultipartFile noType = new MockMultipartFile("file", "a", null, new byte[]{1});

        assertThatThrownBy(() -> service.storePhoto(UUID.randomUUID(), noType))
                .isInstanceOf(InvalidPhotoException.class);
        verifyNoInteractions(photoStorage);
    }

    @Test
    void storePhoto_unknownStudent_returns404WithoutStoring() {
        UUID id = UUID.randomUUID();
        when(studentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.storePhoto(id, new MockMultipartFile("file", "a.png", "image/png", new byte[]{1})))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
        verifyNoInteractions(photoStorage);
    }

    @Test
    void storePhoto_unreadableUpload_isWrappedWithStudentId() throws IOException {
        UUID id = UUID.randomUUID();
        when(studentRepository.findById(id)).thenReturn(Optional.of(Student.builder().id(id).build()));
        MultipartFile broken = mock(MultipartFile.class);
        when(broken.isEmpty()).thenReturn(false);
        when(broken.getContentType()).thenReturn("image/png");
        when(broken.getBytes()).thenThrow(new IOException("disk gone"));

        assertThatThrownBy(() -> service.storePhoto(id, broken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(id.toString())
                .hasCauseInstanceOf(IOException.class);
        verifyNoInteractions(photoStorage);
    }

    @Test
    void getPhoto_returnsBytesContentTypeAndFilename() throws IOException {
        UUID id = UUID.randomUUID();
        StudentPhoto photo = StudentPhoto.builder().data(new byte[]{4, 5}).contentType("image/jpeg")
                .originalFilename("me.jpg").build();
        when(studentRepository.existsById(id)).thenReturn(true);
        when(photoStorage.findByStudentId(id)).thenReturn(Optional.of(photo));

        PhotoResponseDto dto = service.getPhoto(id);

        assertThat(dto.contentType()).isEqualTo("image/jpeg");
        assertThat(dto.filename()).isEqualTo("me.jpg");
        assertThat(dto.resource().getContentAsByteArray()).containsExactly(4, 5);
    }

    @Test
    void getPhoto_unknownStudent_returns404WithoutQueryingStorage() {
        UUID id = UUID.randomUUID();
        when(studentRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.getPhoto(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Student not found");
        verifyNoInteractions(photoStorage);
    }

    @Test
    void deletePhoto_unknownStudent_returns404() {
        UUID id = UUID.randomUUID();
        when(studentRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.deletePhoto(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(photoStorage);
    }

    @Test
    void deletePhotoIfExists_alwaysDelegatesAndNeverChecksTheStudent() {
        UUID id = UUID.randomUUID();

        service.deletePhotoIfExists(id);

        verify(photoStorage).deleteByStudentId(id);
        verifyNoInteractions(studentRepository);
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
