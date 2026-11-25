package com.example.student_management.command;

import com.example.student_management.domain.Student;
import com.example.student_management.service.StudentService;
import com.example.student_management.state.StudentStatus;
import com.example.student_management.state.StudentTransition;

import java.util.UUID;

/**
 * Moves a student to a new status. It remembers the status before the change,
 * and undo puts that status back directly. Some undos are not normal
 * transitions; GRADUATED back to ACTIVE is one example.
 */
public final class ChangeStudentStatusCommand implements StudentCommand<Student> {
    private final StudentService studentService;
    private final UUID studentId;
    private final StudentTransition transition;
    private StudentStatus before;

    public ChangeStudentStatusCommand(StudentService studentService, UUID studentId, StudentTransition transition) {
        this.studentService = studentService;
        this.studentId = studentId;
        this.transition = transition;
    }

    @Override
    public Student execute() {
        before = studentService.getStudentById(studentId).getStatus();
        return studentService.changeStatus(studentId, transition);
    }

    @Override
    public void undo() {
        studentService.restoreStatus(studentId, before);
    }

    @Override
    public String description() {
        return transition.name().charAt(0) + transition.name().substring(1).toLowerCase() + " student " + studentId;
    }
}
