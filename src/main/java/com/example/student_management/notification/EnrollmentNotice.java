package com.example.student_management.notification;

import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import com.example.student_management.event.StudentEvent;
import com.example.student_management.event.StudentEventType;
import com.example.student_management.repository.CourseRepository;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Sent when the student is enrolled in or dropped from a course ({@code relatedId} is the course). */
@Component
public class EnrollmentNotice extends StudentNotification {
    private final CourseRepository courseRepository;

    public EnrollmentNotice(NotificationChannel channel, CourseRepository courseRepository) {
        super(channel);
        this.courseRepository = courseRepository;
    }

    @Override
    public Set<StudentEventType> triggers() {
        return Set.of(StudentEventType.COURSE_ENROLLED, StudentEventType.COURSE_DROPPED);
    }

    @Override
    protected String subject(Student student, StudentEvent event) {
        return (enrolled(event) ? "Enrolled in " : "Dropped from ") + course(event);
    }

    @Override
    protected String body(Student student, StudentEvent event) {
        return enrolled(event)
                ? "You are now enrolled in " + course(event) + "."
                : "You are no longer enrolled in " + course(event) + ". If this is a mistake, contact the office.";
    }

    private static boolean enrolled(StudentEvent event) {
        return event.type() == StudentEventType.COURSE_ENROLLED;
    }

    private String course(StudentEvent event) {
        return event.relatedId() == null ? "a course"
                : courseRepository.findById(event.relatedId())
                        .map(c -> c.getCode() + " " + c.getCourseName()).orElse("a course");
    }
}
