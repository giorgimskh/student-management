package com.example.student_management.controller;

import com.example.student_management.command.AssignBookCommand;
import com.example.student_management.command.ChangeStudentStatusCommand;
import com.example.student_management.command.DropCourseCommand;
import com.example.student_management.command.EnrollInCourseCommand;
import com.example.student_management.command.RemoveBookCommand;
import com.example.student_management.command.StudentCommandInvoker;
import com.example.student_management.command.UpdateStudentCommand;
import com.example.student_management.domain.Book;
import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import com.example.student_management.dto.BookRequest;
import com.example.student_management.dto.BookResponse;
import com.example.student_management.dto.CourseRequest;
import com.example.student_management.dto.CourseResponse;
import com.example.student_management.dto.PagedResponse;
import com.example.student_management.dto.StudentRequest;
import com.example.student_management.dto.StudentResponse;
import com.example.student_management.mapper.EntityMapper;
import com.example.student_management.search.StudentSearch;
import com.example.student_management.service.StudentService;
import com.example.student_management.state.StudentStatus;
import com.example.student_management.state.StudentTransition;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentService studentService;
    private final StudentCommandInvoker commandInvoker;
    private final EntityMapper<Student, StudentRequest, StudentResponse> studentMapper;
    private final EntityMapper<Book, BookRequest, BookResponse> bookMapper;
    private final EntityMapper<Course, CourseRequest, CourseResponse> courseMapper;

    public StudentController(StudentService studentService,
                             StudentCommandInvoker commandInvoker,
                             EntityMapper<Student, StudentRequest, StudentResponse> studentMapper,
                             EntityMapper<Book, BookRequest, BookResponse> bookMapper,
                             EntityMapper<Course, CourseRequest, CourseResponse> courseMapper) {
        this.studentService = studentService;
        this.commandInvoker = commandInvoker;
        this.studentMapper = studentMapper;
        this.bookMapper = bookMapper;
        this.courseMapper = courseMapper;
    }

    /**
     * Lists students, filtered by any combination of: {@code name} and {@code email}
     * (contains, ignoring case), {@code status} (repeat it for "any of"),
     * {@code course} (enrolled in), {@code overdue=true} (has a late book), and
     * {@code createdFrom}/{@code createdTo} (inclusive dates, UTC).
     */
    @GetMapping
    public ResponseEntity<PagedResponse<StudentResponse>> getAllStudents(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) List<StudentStatus> status,
            @RequestParam(required = false) UUID course,
            @RequestParam(defaultValue = "false") boolean overdue,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        StudentSearch search = StudentSearch.builder()
                .nameContains(name)
                .emailContains(email)
                .statuses(status)
                .enrolledIn(course)
                .withOverdueLoans(overdue)
                .createdFrom(createdFrom == null ? null : createdFrom.atStartOfDay(ZoneOffset.UTC).toInstant())
                .createdBefore(createdTo == null ? null : createdTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant())
                .build();
        return ResponseEntity.ok(PagedResponse.from(studentService.findStudents(search, pageable).map(studentMapper::toResponse)));
    }

    /** Name search, kept for existing clients; {@code GET /api/students?name=...} does the same and more. */
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<StudentResponse>> searchStudents(
            @RequestParam String name,
            @RequestParam(required = false) StudentStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        StudentSearch search = StudentSearch.builder().nameContains(name).status(status).build();
        return ResponseEntity.ok(PagedResponse.from(studentService.findStudents(search, pageable).map(studentMapper::toResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable UUID id) {
        return ResponseEntity.ok(studentMapper.toResponse(studentService.getStudentById(id)));
    }

    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody StudentRequest request) {
        return ResponseEntity.ok(studentMapper.toResponse(studentService.createStudent(studentMapper.toEntity(request))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentResponse> updateStudent(@PathVariable UUID id, @Valid @RequestBody StudentRequest request) {
        return ResponseEntity.ok(studentMapper.toResponse(commandInvoker.execute(new UpdateStudentCommand(studentService, id, studentMapper.toEntity(request)))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable UUID id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<StudentResponse> suspendStudent(@PathVariable UUID id) {
        return changeStatus(id, StudentTransition.SUSPEND);
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<StudentResponse> reactivateStudent(@PathVariable UUID id) {
        return changeStatus(id, StudentTransition.REACTIVATE);
    }

    @PostMapping("/{id}/graduate")
    public ResponseEntity<StudentResponse> graduateStudent(@PathVariable UUID id) {
        return changeStatus(id, StudentTransition.GRADUATE);
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<StudentResponse> withdrawStudent(@PathVariable UUID id) {
        return changeStatus(id, StudentTransition.WITHDRAW);
    }

    private ResponseEntity<StudentResponse> changeStatus(UUID id, StudentTransition transition) {
        return ResponseEntity.ok(studentMapper.toResponse(
                commandInvoker.execute(new ChangeStudentStatusCommand(studentService, id, transition))));
    }

    @PostMapping("/{studentId}/books/{bookId}")
    public ResponseEntity<StudentResponse> assignBookToStudent(@PathVariable UUID studentId, @PathVariable UUID bookId) {
        return ResponseEntity.ok(studentMapper.toResponse(commandInvoker.execute(new AssignBookCommand(studentService, studentId, bookId))));
    }

    @GetMapping("/{studentId}/books")
    public ResponseEntity<PagedResponse<BookResponse>> getBooksByStudent(@PathVariable UUID studentId, Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(studentService.getBooksByStudent(studentId, pageable).map(bookMapper::toResponse)));
    }

    @DeleteMapping("/{studentId}/books/{bookId}")
    public ResponseEntity<Void> removeBookFromStudent(@PathVariable UUID studentId, @PathVariable UUID bookId) {
        commandInvoker.execute(new RemoveBookCommand(studentService, studentId, bookId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{studentId}/courses/{courseId}")
    public ResponseEntity<StudentResponse> enrollStudentInCourse(@PathVariable UUID studentId, @PathVariable UUID courseId) {
        return ResponseEntity.ok(studentMapper.toResponse(commandInvoker.execute(new EnrollInCourseCommand(studentService, studentId, courseId))));
    }

    @GetMapping("/{studentId}/courses")
    public ResponseEntity<List<CourseResponse>> getCoursesByStudent(@PathVariable UUID studentId) {
        return ResponseEntity.ok(courseMapper.toResponses(studentService.getCoursesByStudent(studentId)));
    }

    @DeleteMapping("/{studentId}/courses/{courseId}")
    public ResponseEntity<Void> removeStudentFromCourse(@PathVariable UUID studentId, @PathVariable UUID courseId) {
        commandInvoker.execute(new DropCourseCommand(studentService, studentId, courseId));
        return ResponseEntity.noContent().build();
    }
}
