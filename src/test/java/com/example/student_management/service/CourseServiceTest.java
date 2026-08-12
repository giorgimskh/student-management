package com.example.student_management.service;

import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.repository.CourseRepository;
import com.example.student_management.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CourseServiceTest {
    @Mock
    private CourseRepository courseRepository;

    private StudentRepository studentRepository;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository);
    }

    @Test
    public void ddeleteCourse_detachesCourseFromAllStudents_whenCourseExists(){
        UUID courseId=UUID.randomUUID();
        Course course = Course.builder().id(courseId).courseName("Advanced Java").build();

        Student spyStudent1 = spy(Student.builder().id(UUID.randomUUID()).courses(new HashSet<>()).build());
        Student spyStudent2 = spy(Student.builder().id(UUID.randomUUID()).courses(new HashSet<>()).build());

        spyStudent1.getCourses().add(course);
        spyStudent2.getCourses().add(course);

        course.setStudents(new HashSet<>(Set.of(spyStudent1,spyStudent2)));

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        courseService.deleteCourse(courseId);

        assertThat(spyStudent1.getCourses()).doesNotContain(course);
        assertThat(spyStudent2.getCourses()).doesNotContain(course);

        assertThat(course.getStudents()).doesNotContain(spyStudent1,spyStudent2);
        verify(courseRepository).delete(course);
    }

    @Test
    void getStudentsInCourse_throwsResourceNotFoundException_whenCourseIdNotFound() {
        UUID courseId = UUID.randomUUID();

        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getStudentsInCourse(courseId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(courseId.toString());
    }


}
