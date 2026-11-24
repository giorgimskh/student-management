package com.example.student_management.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Observer that writes every student event to the application log. */
@Component
public class LoggingStudentEventListener implements StudentEventListener {
    private static final Logger log = LoggerFactory.getLogger(LoggingStudentEventListener.class);

    @Override
    public void onStudentEvent(StudentEvent event) {
        if (event.relatedId() == null) {
            log.info("[EVENT] {} student={}", event.type(), event.studentId());
        } else {
            log.info("[EVENT] {} student={} related={}", event.type(), event.studentId(), event.relatedId());
        }
    }
}
