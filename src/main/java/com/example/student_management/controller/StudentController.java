package com.example.student_management.controller;

import com.example.student_management.dto.BookResponse;
import com.example.student_management.dto.CourseResponse;
import com.example.student_management.dto.PagedResponse;
import com.example.student_management.dto.StudentRequest;
import com.example.student_management.dto.StudentResponse;
import com.example.student_management.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<StudentResponse>> getAllStudents(@PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(studentService.getAllStudents(pageable).map(StudentResponse::from)));
    }

    @GetMapping("/search")
    public ResponseEntity<PagedResponse<StudentResponse>> searchStudents(
            @RequestParam String name,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(studentService.searchStudents(name, pageable).map(StudentResponse::from)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable UUID id) {
        return ResponseEntity.ok(StudentResponse.from(studentService.getStudentById(id)));
    }

    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody StudentRequest request) {
        return ResponseEntity.ok(StudentResponse.from(studentService.createStudent(request.toEntity())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentResponse> updateStudent(@PathVariable UUID id, @Valid @RequestBody StudentRequest request) {
        return ResponseEntity.ok(StudentResponse.from(studentService.updateStudent(id, request.toEntity())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable UUID id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{studentId}/books/{bookId}")
    public ResponseEntity<StudentResponse> assignBookToStudent(@PathVariable UUID studentId, @PathVariable UUID bookId) {
        return ResponseEntity.ok(StudentResponse.from(studentService.assignBookToStudent(studentId, bookId)));
    }

    @GetMapping("/{studentId}/books")
    public ResponseEntity<PagedResponse<BookResponse>> getBooksByStudent(@PathVariable UUID studentId, Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(studentService.getBooksByStudent(studentId, pageable).map(BookResponse::from)));
    }

    @DeleteMapping("/{studentId}/books/{bookId}")
    public ResponseEntity<Void> removeBookFromStudent(@PathVariable UUID studentId, @PathVariable UUID bookId) {
        studentService.removeBookFromStudent(studentId, bookId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{studentId}/courses/{courseId}")
    public ResponseEntity<StudentResponse> enrollStudentInCourse(@PathVariable UUID studentId, @PathVariable UUID courseId) {
        return ResponseEntity.ok(StudentResponse.from(studentService.enrollInCourse(studentId, courseId)));
    }

    @GetMapping("/{studentId}/courses")
    public ResponseEntity<List<CourseResponse>> getCoursesByStudent(@PathVariable UUID studentId) {
        return ResponseEntity.ok(studentService.getCoursesByStudent(studentId).stream().map(CourseResponse::from).toList());
    }

    @DeleteMapping("/{studentId}/courses/{courseId}")
    public ResponseEntity<Void> removeStudentFromCourse(@PathVariable UUID studentId, @PathVariable UUID courseId) {
        studentService.removeStudentFromCourse(studentId, courseId);
        return ResponseEntity.noContent().build();
    }
}
