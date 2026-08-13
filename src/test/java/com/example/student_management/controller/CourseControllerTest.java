package com.example.student_management.controller;


import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.service.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
public class CourseControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllCourses_returnsOkAndList() throws Exception {
        Course course = Course.builder()
                .id(UUID.randomUUID())
                .courseName("Databases")
                .code("CS201")
                .build();

        when(courseService.getAllCourses()).thenReturn(List.of(course));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseName").value("Databases"))
                .andExpect(jsonPath("$[0].code").value("CS201"));
    }

    @Test
    void getCourseById_returns404_whenServiceThrowsResourceNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(courseService.getCourseById(id))
                .thenThrow(new ResourceNotFoundException("Course not found with id: " + id));

        mockMvc.perform(get("/api/courses/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Course not found with id: " + id));
    }

    @Test
    void createCourse_returns400_whenCourseNameIsBlank() throws Exception {
        // courseName blank -> @Valid should reject before the service is ever called.
        String invalidPayload = """
                {"courseName": "", "code": "CS201"}
                """;

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.courseName").value("Course name is required"));

        verifyNoInteractions(courseService);
    }

    @Test
    void createCourse_returns400_whenCodeIsBlank() throws Exception {
        String invalidPayload = """
                {"courseName": "Databases", "code": ""}
                """;

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Course code is required"));

        verifyNoInteractions(courseService);
    }

    @Test
    void createCourse_returns201_whenValid() throws Exception {
        Course input = Course.builder()
                .courseName("Databases")
                .code("CS201")
                .build();

        Course saved = Course.builder()
                .id(UUID.randomUUID())
                .courseName("Databases")
                .code("CS201")
                .build();

        when(courseService.createCourse(any(Course.class))).thenReturn(saved);

        // Note: unlike StudentController/BookController, CourseController
        // returns 201 Created (HttpStatus.CREATED) on success, not 200.
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.courseName").value("Databases"));
    }

    @Test
    void deleteCourse_returns204_andDelegatesToService() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/courses/{id}", id))
                .andExpect(status().isNoContent());

        verify(courseService).deleteCourse(eq(id));
    }

    @Test
    void deleteCourse_returns404_whenServiceThrowsResourceNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Course not found with id: " + id))
                .when(courseService).deleteCourse(id);

        mockMvc.perform(delete("/api/courses/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Course not found with id: " + id));
    }

    @Test
    void getStudentsInCourse_returnsOkAndSet_whenCourseExists() throws Exception {
        UUID courseId = UUID.randomUUID();
        Student student = Student.builder()
                .id(UUID.randomUUID())
                .name("Nino Kapanadze")
                .email("nino@example.com")
                .build();

        when(courseService.getStudentsInCourse(courseId)).thenReturn(Set.of(student));

        mockMvc.perform(get("/api/courses/{courseId}/students", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Nino Kapanadze"));
    }

    @Test
    void getStudentsInCourse_returns404_whenCourseDoesNotExist() throws Exception {
        UUID courseId = UUID.randomUUID();
        when(courseService.getStudentsInCourse(courseId))
                .thenThrow(new ResourceNotFoundException("Course not found with id: " + courseId));

        mockMvc.perform(get("/api/courses/{courseId}/students", courseId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Course not found with id: " + courseId));
    }

}
