package com.example.student_management.controller;

import com.example.student_management.command.InMemoryStudentCommandInvoker;
import com.example.student_management.command.StudentCommandInvoker;
import com.example.student_management.domain.Student;
import com.example.student_management.exceptions.InvalidStudentStateException;
import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.mapper.BookMapper;
import com.example.student_management.mapper.CourseMapper;
import com.example.student_management.mapper.StudentMapper;
import com.example.student_management.service.StudentService;
import com.example.student_management.state.StudentStatus;
import com.example.student_management.state.StudentTransition;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
@Import({InMemoryStudentCommandInvoker.class, StudentMapper.class, BookMapper.class, CourseMapper.class})
class StudentStatusControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentCommandInvoker commandInvoker;

    @MockitoBean
    private StudentService studentService;

    private static Student student(UUID id, StudentStatus status) {
        return Student.builder().id(id).name("Nino").email("nino@example.com").status(status).build();
    }

    @ParameterizedTest
    @CsvSource({
            "suspend,    SUSPEND,    ACTIVE,    SUSPENDED",
            "reactivate, REACTIVATE, SUSPENDED, ACTIVE",
            "graduate,   GRADUATE,   ACTIVE,    GRADUATED",
            "withdraw,   WITHDRAW,   ACTIVE,    WITHDRAWN"
    })
    void transitionEndpoint_returnsStudentWithNewStatus(String path, StudentTransition transition,
                                                       StudentStatus from, StudentStatus to) throws Exception {
        UUID id = UUID.randomUUID();
        when(studentService.getStudentById(id)).thenReturn(student(id, from));
        when(studentService.changeStatus(id, transition)).thenReturn(student(id, to));

        mockMvc.perform(post("/api/students/{id}/" + path, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value(to.name()));

        verify(studentService).changeStatus(id, transition);
    }

    @Test
    void transitionNotAllowed_returns409WithMessage() throws Exception {
        UUID id = UUID.randomUUID();
        when(studentService.getStudentById(id)).thenReturn(student(id, StudentStatus.GRADUATED));
        when(studentService.changeStatus(id, StudentTransition.SUSPEND))
                .thenThrow(new InvalidStudentStateException("Cannot suspend a student who is GRADUATED"));
        int historyBefore = commandInvoker.history().size();

        mockMvc.perform(post("/api/students/{id}/suspend", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Cannot suspend a student who is GRADUATED"));

        assertThat(commandInvoker.history()).hasSize(historyBefore); // a failed command is not undoable
    }

    @Test
    void unknownStudent_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(studentService.getStudentById(id)).thenThrow(new ResourceNotFoundException("Student not found with id: " + id));

        mockMvc.perform(post("/api/students/{id}/withdraw", id))
                .andExpect(status().isNotFound());

        verify(studentService, never()).changeStatus(any(), any());
    }

    @Test
    void operationForbiddenByState_returns409() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        when(studentService.enrollInCourse(studentId, courseId))
                .thenThrow(new InvalidStudentStateException("Student " + studentId + " is SUSPENDED and cannot enroll in courses"));

        mockMvc.perform(post("/api/students/{s}/courses/{c}", studentId, courseId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Student " + studentId + " is SUSPENDED and cannot enroll in courses"));
    }

    @Test
    void getStudent_includesStatus() throws Exception {
        UUID id = UUID.randomUUID();
        when(studentService.getStudentById(id)).thenReturn(student(id, StudentStatus.SUSPENDED));

        mockMvc.perform(get("/api/students/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }
}
