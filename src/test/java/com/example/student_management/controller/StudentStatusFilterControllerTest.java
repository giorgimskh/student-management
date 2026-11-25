package com.example.student_management.controller;

import com.example.student_management.command.InMemoryStudentCommandInvoker;
import com.example.student_management.domain.Student;
import com.example.student_management.mapper.BookMapper;
import com.example.student_management.mapper.CourseMapper;
import com.example.student_management.mapper.StudentMapper;
import com.example.student_management.service.StudentService;
import com.example.student_management.state.StudentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
@Import({InMemoryStudentCommandInvoker.class, StudentMapper.class, BookMapper.class, CourseMapper.class})
class StudentStatusFilterControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    private static PageImpl<Student> onePage(StudentStatus status) {
        return new PageImpl<>(List.of(Student.builder().id(UUID.randomUUID()).name("Nino").email("n@example.com").status(status).build()));
    }

    @Test
    void list_withStatus_filtersOnTheServer() throws Exception {
        when(studentService.findStudents(isNull(), eq(StudentStatus.SUSPENDED), any(Pageable.class))).thenReturn(onePage(StudentStatus.SUSPENDED));

        mockMvc.perform(get("/api/students").param("status", "SUSPENDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("SUSPENDED"));
        verify(studentService, never()).getAllStudents(any(Pageable.class));
    }

    @Test
    void list_withoutStatus_keepsTheOldCall() throws Exception {
        when(studentService.getAllStudents(any(Pageable.class))).thenReturn(onePage(StudentStatus.ACTIVE));

        mockMvc.perform(get("/api/students")).andExpect(status().isOk());
        verify(studentService, never()).findStudents(any(), any(), any());
    }

    @Test
    void search_withStatus() throws Exception {
        when(studentService.findStudents(eq("nin"), eq(StudentStatus.GRADUATED), any(Pageable.class))).thenReturn(onePage(StudentStatus.GRADUATED));

        mockMvc.perform(get("/api/students/search").param("name", "nin").param("status", "GRADUATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("GRADUATED"));
    }

    @Test
    void unknownStatus_returns400() throws Exception {
        mockMvc.perform(get("/api/students").param("status", "ON_HOLIDAY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value for parameter 'status'"));
        verifyNoInteractions(studentService);
    }
}
