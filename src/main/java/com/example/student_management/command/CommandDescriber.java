package com.example.student_management.command;

import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import com.example.student_management.repository.BookRepository;
import com.example.student_management.repository.CourseRepository;
import com.example.student_management.repository.StudentRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Visitor that describes a command with names instead of ids, e.g.
 * {@code Assign "Clean Code" to Ana}. Names are looked up when the description
 * is made, so they show current values; anything deleted since falls back to its id.
 */
@Component
public class CommandDescriber implements StudentCommandVisitor<String> {
    private final StudentRepository studentRepository;
    private final BookRepository bookRepository;
    private final CourseRepository courseRepository;

    public CommandDescriber(StudentRepository studentRepository,
                            BookRepository bookRepository,
                            CourseRepository courseRepository) {
        this.studentRepository = studentRepository;
        this.bookRepository = bookRepository;
        this.courseRepository = courseRepository;
    }

    /** Convenience for {@code command.accept(this)}. */
    public String describe(StudentCommand<?> command) {
        return command.accept(this);
    }

    @Override
    public String visit(AssignBookCommand command) {
        return "Assign " + book(command.bookId()) + " to " + student(command.studentId());
    }

    @Override
    public String visit(RemoveBookCommand command) {
        return "Remove " + book(command.bookId()) + " from " + student(command.studentId());
    }

    @Override
    public String visit(EnrollInCourseCommand command) {
        return "Enroll " + student(command.studentId()) + " in " + course(command.courseId());
    }

    @Override
    public String visit(DropCourseCommand command) {
        return "Drop " + student(command.studentId()) + " from " + course(command.courseId());
    }

    @Override
    public String visit(UpdateStudentCommand command) {
        return "Update profile of " + student(command.studentId());
    }

    @Override
    public String visit(ChangeStudentStatusCommand command) {
        String verb = command.transition().name();
        return verb.charAt(0) + verb.substring(1).toLowerCase() + " " + student(command.studentId());
    }

    private String student(UUID id) {
        return studentRepository.findById(id).map(Student::getName).orElse("student " + id);
    }

    private String book(UUID id) {
        return bookRepository.findById(id).map(b -> "\"" + b.getTitle() + "\"").orElse("book " + id);
    }

    private String course(UUID id) {
        return courseRepository.findById(id).map(Course::label).orElse("course " + id);
    }
}
