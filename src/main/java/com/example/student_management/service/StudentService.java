package com.example.student_management.service;

import com.example.student_management.domain.Student;
import com.example.student_management.repository.BookRepository;
import com.example.student_management.repository.CourseRepository;
import com.example.student_management.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import com.example.student_management.domain.Book;

@Service
public class StudentService {
    @Autowired
    private StudentRepository studentRepository;
    private BookRepository bookRepository;
    private CourseRepository courseRepository;

    public StudentService(StudentRepository studentRepository, BookRepository bookRepository, CourseRepository courseRepository) {
        this.studentRepository = studentRepository;
        this.bookRepository = bookRepository;
        this.courseRepository = courseRepository;
    }

    public List<Student> getAllStudents(){
        return studentRepository.findAll();
    }

    public Student  createStudent(Student student){
        return studentRepository.save(student);
    }

    public Student getStudentById(UUID id){
        return studentRepository.findById(id).orElseThrow(()->new RuntimeException("Student not found with id: " + id));
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
        Book book =bookRepository.findById(bookId).orElseThrow(()->new RuntimeException("Book not found with id: " + bookId));

        book.setStudent(student);
        student.getBooks().add(book);

        bookRepository.save(book);

        return student;
    }
}
