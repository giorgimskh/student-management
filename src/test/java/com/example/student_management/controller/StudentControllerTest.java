package com.example.student_management.controller;

import com.example.student_management.domain.Student;
import com.example.student_management.exceptions.DuplicateEmailException;
import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;


import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
public class StudentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudentService studentService;

    @Test
    public void getAllStudents_returnsOkAndList() throws Exception {
        Student s = Student.builder()
                .id(UUID.randomUUID())
                .name("Nino Kapanadze")
                .email("nino@example.com")
                .build();

        when(studentService.getAllStudents()).thenReturn(List.of(s));

        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Nino Kapanadze"))
                .andExpect(jsonPath("$[0].email").value("nino@example.com"));
    }

    @Test
    void getStudentById_returns404_whenServiceThrowsResourceNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(studentService.getStudentById(id))
                .thenThrow(new ResourceNotFoundException("Student not found with id: " + id));

        mockMvc.perform(get("/api/students/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Student not found with id: " + id));
    }

    @Test
    public void createStudent_returns400_whenNameIsBlank() throws Exception {
        String invalidPayload = """
                {"name": "", "email": "not-an-email"}
                """;

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.email").exists());

        verifyNoInteractions(studentService);

    }

    @Test
    void createStudent_returns200_andEchoesCreatedStudent_whenValid() throws Exception{
        Student input=Student.builder()
                .name("Nino Kapanadze")
                .email("nino@example.com")
                .build();

        Student saved = Student.builder()
                .id(UUID.randomUUID())
                .name("Nino Kapanadze")
                .email("nino@example.com")
                .build();

        when(studentService.createStudent(any(Student.class))).thenReturn(saved);

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.email").value("nino@example.com"));
    }

    @Test
    void createStudent_returns409_whenEmailAlreadyExists() throws Exception {
        Student input = Student.builder()
                .name("Nino Kapanadze")
                .email("nino@example.com")
                .build();

        when(studentService.createStudent(any(Student.class)))
                .thenThrow(new DuplicateEmailException("A student with email 'nino@example.com' already exists."));

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("A student with email 'nino@example.com' already exists."));
    }

    @Test
    void deleteStudent_returns204_andDelegatesToService() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/students/{id}", id))
                .andExpect(status().isNoContent());

        verify(studentService).deleteStudent(eq(id));
    }

}
