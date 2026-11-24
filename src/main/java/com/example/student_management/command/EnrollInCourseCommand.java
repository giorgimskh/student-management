package com.example.student_management.command;

import com.example.student_management.domain.Student;
import com.example.student_management.service.StudentService;

import java.util.UUID;

public record EnrollInCourseCommand(StudentService studentService, UUID studentId, UUID courseId)
        implements StudentCommand<Student> {

    @Override
    public Student execute() {
        return studentService.enrollInCourse(studentId, courseId);
    }

    @Override
    public void undo() {
        studentService.removeStudentFromCourse(studentId, courseId);
    }

    @Override
    public String description() {
        return "Enroll student " + studentId + " in course " + courseId;
    }
}
