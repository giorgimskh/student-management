package com.example.student_management.service;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import com.example.student_management.domain.StudentMemento;
import com.example.student_management.search.StudentSearch;
import com.example.student_management.state.StudentStatus;
import com.example.student_management.state.StudentTransition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Business operations on students, their books and their course enrollments. */
public interface StudentService {
    List<Student> getAllStudents();

    Page<Student> getAllStudents(Pageable pageable);

    /** Students whose name contains {@code name}, ignoring case. Same as {@link #findStudents(StudentSearch, Pageable)} with only a name. */
    Page<Student> searchStudents(String name, Pageable pageable);

    /**
     * Students filtered by name (contains, ignoring case) and/or status. Either
     * filter may be null or blank, in which case it is not applied.
     */
    Page<Student> findStudents(String name, StudentStatus status, Pageable pageable);

    /** Students matching every condition set on {@code search}; "overdue" is judged against the current time. */
    Page<Student> findStudents(StudentSearch search, Pageable pageable);

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

    /**
     * Moves the student to a new status. Fails with 409 if the current status
     * does not allow {@code transition}.
     */
    Student changeStatus(UUID id, StudentTransition transition);

    /**
     * Puts the student back to {@code status} without checking transition rules.
     * Used to undo {@link #changeStatus}, e.g. GRADUATED back to ACTIVE.
     */
    Student restoreStatus(UUID id, StudentStatus status);
}
