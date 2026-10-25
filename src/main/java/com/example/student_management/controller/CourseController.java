package com.example.student_management.controller;

import com.example.student_management.dto.CourseRequest;
import com.example.student_management.dto.CourseResponse;
import com.example.student_management.dto.PagedResponse;
import com.example.student_management.dto.StudentResponse;
import com.example.student_management.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<CourseResponse>> getAllCourses(@PageableDefault(size = 10, sort = "courseName") Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(courseService.getAllCourses(pageable).map(CourseResponse::from)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourseById(@PathVariable UUID id) {
        return ResponseEntity.ok(CourseResponse.from(courseService.getCourseById(id)));
    }

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CourseRequest request) {
        return new ResponseEntity<>(CourseResponse.from(courseService.createCourse(request.toEntity())), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable UUID id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{courseId}/students")
    public ResponseEntity<List<StudentResponse>> getStudentsInCourse(@PathVariable UUID courseId) {
        return ResponseEntity.ok(courseService.getStudentsInCourse(courseId).stream().map(StudentResponse::from).toList());
    }
}
