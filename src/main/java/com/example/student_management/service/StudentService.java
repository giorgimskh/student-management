package com.example.student_management.service;

import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
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
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.example.student_management.domain.Book;

@Service
public class StudentService {
    private static final Logger log = LoggerFactory.getLogger(StudentService.class);
    private final StudentRepository studentRepository;
    private final BookRepository bookRepository;
    private final CourseRepository courseRepository;
    private final Clock clock;

    public StudentService(StudentRepository studentRepository, BookRepository bookRepository, CourseRepository courseRepository,Clock clock) {
        this.studentRepository = studentRepository;
        this.bookRepository = bookRepository;
        this.courseRepository = courseRepository;
        this.clock=clock;
    }

    @PostConstruct
    public void init() {
        log.info("StudentService initialized successfully and ready to process requests.");
    }

    public List<Student> getAllStudents(){
        return studentRepository.findAll();
    }

    public Student  createStudent(Student student){
        if(studentRepository.findByEmail(student.getEmail()).isPresent())
            throw new DuplicateEmailException("A student with email '" + student.getEmail() + "' already exists.");

        student.setCreatedAt(Instant.now(clock));
        return studentRepository.save(student);
    }

    public Student getStudentById(UUID id){
        return studentRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Student not found with id: " + id));
    }

    public Student updateStudent(UUID id, Student studentDetails) {
        Student student = getStudentById(id);
        student.setName(studentDetails.getName());
        student.setEmail(studentDetails.getEmail());
        return studentRepository.save(student);
    }

    public void deleteStudent(UUID id){
        studentRepository.deleteById(id);
    }

    public Student assignBookToStudent(UUID studentId,UUID bookId){
        Student student = getStudentById(studentId);
        Book book =bookRepository.findById(bookId).orElseThrow(()->new ResourceNotFoundException("Book not found with id: " + bookId));

        if(book.getStudent()!=null)
            throw new BookAlreadyAssignedException("Book is already assigned to student");
        book.setStudent(student);
        student.getBooks().add(book);

        bookRepository.save(book);

        return student;
    }

    public List<Book> getBooksByStudent(UUID studentId){
        Student student = getStudentById(studentId);
        return student.getBooks();
    }

    public void removeBookFromStudent(UUID studentId,UUID bookId){
        Book book = bookRepository.findById(bookId).orElseThrow(()->new ResourceNotFoundException("Book with such id not found"));
        book.setStudent(null);
        bookRepository.save(book);
    }

    @Transactional
    public Student enrollInCourse(UUID studentId,UUID courseId){
        Student student=getStudentById(studentId);
        Course course=courseRepository.findById(courseId).orElseThrow(()->new ResourceNotFoundException("Course not found with id: "+courseId));

        if(course.getStudents().contains(student))
            throw new InvalidEnrollmentException("Student " + student.getName() + " is already enrolled in " + course.getCourseName());

        student.getCourses().add(course);
        course.getStudents().add(student);
        return studentRepository.save(student);
    }

    public Set<Course> getCoursesByStudent(UUID studentId){
        Student student=getStudentById(studentId);
        return student.getCourses();
    }

    public void removeStudentFromCourse(UUID studentId,UUID courseId){
        Student student=getStudentById(studentId);
        Course course=courseRepository.findById(courseId).orElseThrow(()->new ResourceNotFoundException("Course not found with id"+courseId));
        student.getCourses().remove(course);
        course.getStudents().remove(student);
        studentRepository.save(student);
    }
}
