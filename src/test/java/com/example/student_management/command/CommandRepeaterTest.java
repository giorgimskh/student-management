package com.example.student_management.command;

import com.example.student_management.command.CommandRepeater.Outcome;
import com.example.student_management.command.CommandRepeater.Result;
import com.example.student_management.exceptions.CommandNotRepeatableException;
import com.example.student_management.exceptions.InvalidEnrollmentException;
import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandRepeaterTest {
    @Mock
    private StudentService studentService;
    @Mock
    private CommandDescriber describer;

    private final StudentCommandInvoker invoker = new InMemoryStudentCommandInvoker();
    private CommandRepeater repeater;

    private final UUID ana = UUID.randomUUID();
    private final UUID beka = UUID.randomUUID();
    private final UUID gio = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repeater = new CommandRepeater(invoker, describer);
        lenient().when(describer.describe(any())).thenAnswer(inv -> "did " + ((StudentCommand<?>) inv.getArgument(0)).studentId());
    }

    private long enrollAna() {
        invoker.execute(new EnrollInCourseCommand(studentService, ana, courseId));
        return invoker.entries().getFirst().id();
    }

    @Test
    void runsACopyPerStudent_eachRecordedAndUndoableOnItsOwn() {
        long id = enrollAna();

        List<Result> results = repeater.repeat(id, List.of(beka, gio));

        assertThat(results).containsExactly(
                new Result(beka, Outcome.DONE, "did " + beka),
                new Result(gio, Outcome.DONE, "did " + gio));
        verify(studentService).enrollInCourse(beka, courseId);
        verify(studentService).enrollInCourse(gio, courseId);
        assertThat(invoker.commands()).extracting(StudentCommand::studentId).containsExactly(gio, beka, ana);

        invoker.undoLast();
        verify(studentService).removeStudentFromCourse(gio, courseId);
    }

    @Test
    void oneStudentFailing_doesNotStopTheOthers_andIsNotRecorded() {
        long id = enrollAna();
        when(studentService.enrollInCourse(beka, courseId)).thenThrow(new InvalidEnrollmentException("Beka is already enrolled"));

        List<Result> results = repeater.repeat(id, List.of(beka, gio));

        assertThat(results).containsExactly(
                new Result(beka, Outcome.FAILED, "Beka is already enrolled"),
                new Result(gio, Outcome.DONE, "did " + gio));
        assertThat(invoker.commands()).hasSize(2);
    }

    @Test
    void unexpectedErrors_areNotShownToTheClient() {
        long id = enrollAna();
        when(studentService.enrollInCourse(beka, courseId)).thenThrow(new IllegalStateException("connection pool exhausted"));

        assertThat(repeater.repeat(id, List.of(beka)))
                .containsExactly(new Result(beka, Outcome.FAILED, "Unexpected error"));
    }

    @Test
    void theOriginalStudent_isSkipped_andDuplicatesAreIgnored() {
        long id = enrollAna();

        List<Result> results = repeater.repeat(id, List.of(ana, beka, beka));

        assertThat(results).extracting(Result::studentId, Result::outcome)
                .containsExactly(tuple(ana, Outcome.SKIPPED), tuple(beka, Outcome.DONE));
        verify(studentService).enrollInCourse(ana, courseId); // only the original run
        verify(studentService).enrollInCourse(beka, courseId);
        verifyNoMoreInteractions(studentService);
    }

    @Test
    void unknownEntry_isNotFound() {
        assertThatThrownBy(() -> repeater.repeat(42, List.of(beka)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No command #42 in the history");
    }

    @Test
    void notRepeatableCommand_isRefused_beforeAnythingRuns() {
        invoker.execute(new AssignBookCommand(studentService, ana, UUID.randomUUID()));
        long id = invoker.entries().getFirst().id();

        assertThatThrownBy(() -> repeater.repeat(id, List.of(beka)))
                .isInstanceOf(CommandNotRepeatableException.class)
                .hasMessageContaining("only one student");
        assertThat(invoker.commands()).hasSize(1);
    }
}
