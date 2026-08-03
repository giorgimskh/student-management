package com.example.student_management.controller;

import com.example.student_management.domain.Course;
import com.example.student_management.service.StudentService;
import com.example.student_management.domain.Student;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.example.student_management.domain.Book;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents(){
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    @PostMapping
    public ResponseEntity<Student> createStudent(@RequestBody Student student){
        return ResponseEntity.ok(studentService.createStudent(student));
    }

    @GetMapping("/id")
    public ResponseEntity<Student> getStudentById(@PathVariable UUID id){
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @PutMapping("/id")
    public ResponseEntity<Student> updateStudent(@PathVariable UUID id,@RequestBody Student student){
        return ResponseEntity.ok(studentService.updateStudent(id,student));
    }

    @DeleteMapping("/id")
    public ResponseEntity<Void> deleteStudent(@PathVariable UUID id){
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{studentId}/books/{bookId}")
    public ResponseEntity<Student> assignBookToStudent(@PathVariable UUID studentId,@PathVariable UUID bookId){
        return ResponseEntity.ok(studentService.assignBookToStudent(studentId,bookId));
    }

    @GetMapping("/{studentId}/books")
    public ResponseEntity<List<Book>> getBookByStudent(@PathVariable UUID studentId){
        return ResponseEntity.ok(studentService.getBooksByStudent(studentId));
    }

    @DeleteMapping("/{studentId}/books/{bookId}")
    public ResponseEntity<Void> removeBookFromStudent(@PathVariable UUID studentId,@PathVariable UUID bookId){
        studentService.removeBookFromStudent(studentId,bookId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{studentId}/courses/{courseId}")
    public ResponseEntity<Student> enrollStudentInCourse(@PathVariable UUID studentId, @PathVariable UUID courseId){
        return ResponseEntity.ok(studentService.enrollInCourse(studentId,courseId));
    }

    @GetMapping("/{studentId}/courses")
    public ResponseEntity<Set<Course>> getCoursesByStudent(@PathVariable UUID studentId){
        return ResponseEntity.ok(studentService.getCoursesByStudent(studentId));
    }

    @DeleteMapping("/{studentId}/courses/{courseId}")
    public ResponseEntity<Void> removeStudentFromCourse(@PathVariable UUID studentId,@PathVariable UUID courseId){
        studentService.removeBookFromStudent(studentId,courseId);
        return ResponseEntity.noContent().build();
    }

}
