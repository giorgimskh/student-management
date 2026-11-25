package com.example.student_management.service;

import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Business operations on courses. */
public interface CourseService {
    List<Course> getAllCourses();

    Page<Course> getAllCourses(Pageable pageable);

    Course getCourseById(UUID id);

    Course createCourse(Course course);

    void deleteCourse(UUID id);

    Set<Student> getStudentsInCourse(UUID courseId);
}
