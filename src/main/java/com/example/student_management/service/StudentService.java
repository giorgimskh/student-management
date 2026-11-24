package com.example.student_management.service;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import com.example.student_management.event.StudentEventPublisher;
import com.example.student_management.event.StudentEventType;
import com.example.student_management.exceptions.BookAlreadyAssignedException;
import com.example.student_management.exceptions.DuplicateEmailException;
import com.example.student_management.exceptions.InvalidEnrollmentException;
import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.repository.BookRepository;
import com.example.student_management.repository.CourseRepository;
import com.example.student_management.repository.StudentRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class StudentService {
    private static final Logger log = LoggerFactory.getLogger(StudentService.class);
    private final StudentRepository studentRepository;
    private final BookRepository bookRepository;
    private final CourseRepository courseRepository;
    private final Clock clock;
    private final StudentPhotoService studentPhotoService;
    private final StudentEventPublisher eventPublisher;

    public StudentService(StudentRepository studentRepository, BookRepository bookRepository, CourseRepository courseRepository, Clock clock, StudentPhotoService studentPhotoService, StudentEventPublisher eventPublisher) {
        this.studentRepository = studentRepository;
        this.bookRepository = bookRepository;
        this.courseRepository = courseRepository;
        this.clock = clock;
        this.studentPhotoService = studentPhotoService;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void init() {
        log.info("StudentService initialized successfully and ready to process requests.");
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Page<Student> getAllStudents(Pageable pageable) {
        return studentRepository.findAll(pageable);
    }

    public Student createStudent(Student student) {
        if (studentRepository.findByEmail(student.getEmail()).isPresent()) {
            throw new DuplicateEmailException("A student with email '" + student.getEmail() + "' already exists.");
        }

        student.setCreatedAt(Instant.now(clock));
        Student saved = studentRepository.save(student);
        eventPublisher.publish(StudentEventType.STUDENT_CREATED, saved.getId());
        return saved;
    }

    public Student getStudentById(UUID id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
    }

    public Student updateStudent(UUID id, Student studentDetails) {
        Student student = getStudentById(id);

        studentRepository.findByEmail(studentDetails.getEmail())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateEmailException("A student with email '" + studentDetails.getEmail() + "' already exists.");
                });

        student.setName(studentDetails.getName());
        student.setEmail(studentDetails.getEmail());
        Student saved = studentRepository.save(student);
        eventPublisher.publish(StudentEventType.STUDENT_UPDATED, id);
        return saved;
    }

    @Transactional
    public void deleteStudent(UUID id) {
        Student student = getStudentById(id); // 404 instead of a silent no-op for unknown ids
        studentRepository.delete(student);
        studentPhotoService.deletePhotoIfExists(id);
        eventPublisher.publish(StudentEventType.STUDENT_DELETED, id);
    }

    public Student assignBookToStudent(UUID studentId, UUID bookId) {
        Student student = getStudentById(studentId);
        Book book = findBook(bookId);

        if (book.getStudent() != null) {
            throw new BookAlreadyAssignedException("Book is already assigned to student");
        }

        book.setStudent(student);
        student.getBooks().add(book);
        bookRepository.save(book);
        eventPublisher.publish(StudentEventType.BOOK_ASSIGNED, studentId, bookId);
        return student;
    }

    public List<Book> getBooksByStudent(UUID studentId) {
        return getStudentById(studentId).getBooks();
    }

    public Page<Book> getBooksByStudent(UUID studentId, Pageable pageable) {
        getStudentById(studentId); // throws 404 if the student does not exist
        return bookRepository.findByStudentId(studentId, pageable);
    }

    public void removeBookFromStudent(UUID studentId, UUID bookId) {
        getStudentById(studentId);
        Book book = findBook(bookId);

        if (book.getStudent() == null || !studentId.equals(book.getStudent().getId())) {
            throw new ResourceNotFoundException("Book " + bookId + " is not assigned to student " + studentId);
        }

        book.setStudent(null);
        bookRepository.save(book);
        eventPublisher.publish(StudentEventType.BOOK_REMOVED, studentId, bookId);
    }

    @Transactional
    public Student enrollInCourse(UUID studentId, UUID courseId) {
        Student student = getStudentById(studentId);
        Course course = findCourse(courseId);

        if (course.getStudents().contains(student)) {
            throw new InvalidEnrollmentException("Student " + student.getName() + " is already enrolled in " + course.getCourseName());
        }

        student.getCourses().add(course);
        course.getStudents().add(student);
        Student saved = studentRepository.save(student);
        eventPublisher.publish(StudentEventType.COURSE_ENROLLED, studentId, courseId);
        return saved;
    }

    public Set<Course> getCoursesByStudent(UUID studentId) {
        return getStudentById(studentId).getCourses();
    }

    public void removeStudentFromCourse(UUID studentId, UUID courseId) {
        Student student = getStudentById(studentId);
        Course course = findCourse(courseId);
        student.getCourses().remove(course);
        course.getStudents().remove(student);
        studentRepository.save(student);
        eventPublisher.publish(StudentEventType.COURSE_DROPPED, studentId, courseId);
    }

    public Page<Student> searchStudents(String name, Pageable pageable) {
        return studentRepository.findByNameContainingIgnoreCase(name, pageable);
    }

    private Book findBook(UUID bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));
    }

    private Course findCourse(UUID courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
    }
}
