package com.example.student_management.controller;

import com.example.student_management.dto.PhotoResponseDto;
import com.example.student_management.exceptions.InvalidPhotoException;
import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.service.StudentPhotoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentPhotoController.class)
class StudentPhotoControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentPhotoService studentPhotoService;

    private final UUID studentId = UUID.randomUUID();

    private String photoUrl() {
        return "/api/students/" + studentId + "/photo";
    }

    @Test
    void uploadPhoto_returns201AndPassesFileToService() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "me.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart(photoUrl()).file(file))
                .andExpect(status().isCreated());

        verify(studentPhotoService).storePhoto(eq(studentId),
                argThat((MultipartFile f) -> "me.png".equals(f.getOriginalFilename())));
    }

    @Test
    void uploadPhoto_withoutFilePart_returns400() throws Exception {
        mockMvc.perform(multipart(photoUrl()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(studentPhotoService);
    }

    @Test
    void uploadPhoto_invalidPhoto_returns400WithMessage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", new byte[]{1});
        when(studentPhotoService.storePhoto(eq(studentId), any()))
                .thenThrow(new InvalidPhotoException("Photo must be an image"));

        mockMvc.perform(multipart(photoUrl()).file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Photo must be an image"));
    }

    @Test
    void uploadPhoto_unknownStudent_returns404() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "me.png", "image/png", new byte[]{1});
        when(studentPhotoService.storePhoto(eq(studentId), any()))
                .thenThrow(new ResourceNotFoundException("Student not found with id: " + studentId));

        mockMvc.perform(multipart(photoUrl()).file(file))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadPhoto_returnsBytesWithContentTypeAndFilename() throws Exception {
        byte[] bytes = {7, 8, 9};
        when(studentPhotoService.getPhoto(studentId))
                .thenReturn(new PhotoResponseDto(new ByteArrayResource(bytes), "image/jpeg", "me.jpg"));

        mockMvc.perform(get(photoUrl()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"me.jpg\""))
                .andExpect(content().bytes(bytes));
    }

    @Test
    void downloadPhoto_missingPhoto_returns404() throws Exception {
        when(studentPhotoService.getPhoto(studentId))
                .thenThrow(new ResourceNotFoundException("Photo not found for student id: " + studentId));

        mockMvc.perform(get(photoUrl()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Photo not found for student id: " + studentId));
    }

    @Test
    void deletePhoto_returns204() throws Exception {
        mockMvc.perform(delete(photoUrl()))
                .andExpect(status().isNoContent());

        verify(studentPhotoService).deletePhoto(studentId);
    }

    @Test
    void deletePhoto_missingPhoto_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Photo not found")).when(studentPhotoService).deletePhoto(studentId);

        mockMvc.perform(delete(photoUrl()))
                .andExpect(status().isNotFound());
    }
}
