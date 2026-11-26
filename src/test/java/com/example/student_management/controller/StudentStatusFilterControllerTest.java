package com.example.student_management.controller;

import com.example.student_management.command.InMemoryStudentCommandInvoker;
import com.example.student_management.domain.Student;
import com.example.student_management.mapper.BookMapper;
import com.example.student_management.mapper.CourseMapper;
import com.example.student_management.mapper.StudentMapper;
import com.example.student_management.search.StudentSearch;
import com.example.student_management.service.StudentService;
import com.example.student_management.state.StudentStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    /** Runs the request and returns the search the controller built. */
    private StudentSearch searchFor(MockHttpServletRequestBuilder request) throws Exception {
        ArgumentCaptor<StudentSearch> captor = ArgumentCaptor.forClass(StudentSearch.class);
        when(studentService.findStudents(captor.capture(), any(Pageable.class))).thenReturn(onePage(StudentStatus.ACTIVE));
        mockMvc.perform(request).andExpect(status().isOk());
        return captor.getValue();
    }

    @Test
    void list_withStatus_filtersOnTheServer() throws Exception {
        when(studentService.findStudents(any(StudentSearch.class), any(Pageable.class))).thenReturn(onePage(StudentStatus.SUSPENDED));

        mockMvc.perform(get("/api/students").param("status", "SUSPENDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("SUSPENDED"));
        verify(studentService, never()).getAllStudents(any(Pageable.class));
    }

    @Test
    void list_withoutFilters_isAnUnrestrictedSearch() throws Exception {
        assertThat(searchFor(get("/api/students")).isUnrestricted()).isTrue();
    }

    @Test
    void list_passesEveryFilterToTheBuilder() throws Exception {
        UUID course = UUID.randomUUID();

        StudentSearch search = searchFor(get("/api/students")
                .param("name", " nin ").param("email", "example.com")
                .param("status", "ACTIVE").param("status", "SUSPENDED")
                .param("course", course.toString()).param("overdue", "true")
                .param("createdFrom", "2026-01-01").param("createdTo", "2026-01-31"));

        assertThat(search.nameContains()).isEqualTo("nin");
        assertThat(search.emailContains()).isEqualTo("example.com");
        assertThat(search.statuses()).containsExactlyInAnyOrder(StudentStatus.ACTIVE, StudentStatus.SUSPENDED);
        assertThat(search.courseId()).isEqualTo(course);
        assertThat(search.overdueOnly()).isTrue();
        assertThat(search.createdFrom()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(search.createdBefore()).isEqualTo(Instant.parse("2026-02-01T00:00:00Z")); // "to" is inclusive
    }

    @Test
    void list_withAnEmptyDateRange_returns400() throws Exception {
        mockMvc.perform(get("/api/students").param("createdFrom", "2026-02-01").param("createdTo", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("The creation date range is empty: 'from' must be before 'to'"));
        verifyNoInteractions(studentService);
    }

    @Test
    void search_withStatus() throws Exception {
        StudentSearch search = searchFor(get("/api/students/search").param("name", "nin").param("status", "GRADUATED"));

        assertThat(search.nameContains()).isEqualTo("nin");
        assertThat(search.statuses()).containsExactly(StudentStatus.GRADUATED);
    }

    @Test
    void unknownStatus_returns400() throws Exception {
        mockMvc.perform(get("/api/students").param("status", "ON_HOLIDAY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value for parameter 'status'"));
        verifyNoInteractions(studentService);
    }
}
