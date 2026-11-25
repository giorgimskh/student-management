package com.example.student_management.service;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import com.example.student_management.domain.StudentMemento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Business operations on students, their books and their course enrollments. */
public interface StudentService {
    List<Student> getAllStudents();

    Page<Student> getAllStudents(Pageable pageable);

    Page<Student> searchStudents(String name, Pageable pageable);

    Student createStudent(Student student);

    Student getStudentById(UUID id);

    Student updateStudent(UUID id, Student studentDetails);

    /**
     * Restores a student's profile from a snapshot taken earlier with
     * {@link Student#createMemento()}.
     */
    Student restoreStudent(UUID id, StudentMemento memento);

    void deleteStudent(UUID id);

    Student assignBookToStudent(UUID studentId, UUID bookId);

    List<Book> getBooksByStudent(UUID studentId);

    Page<Book> getBooksByStudent(UUID studentId, Pageable pageable);

    void removeBookFromStudent(UUID studentId, UUID bookId);

    Student enrollInCourse(UUID studentId, UUID courseId);

    Set<Course> getCoursesByStudent(UUID studentId);

    void removeStudentFromCourse(UUID studentId, UUID courseId);
}
