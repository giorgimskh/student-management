package com.example.student_management.command;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import com.example.student_management.exceptions.InvalidStudentStateException;
import com.example.student_management.repository.BookRepository;
import com.example.student_management.repository.CourseRepository;
import com.example.student_management.repository.StudentRepository;
import com.example.student_management.service.StudentService;
import com.example.student_management.state.StudentPermission;
import com.example.student_management.state.StudentStatus;
import com.example.student_management.state.StudentTransition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandVisitorTest {
    @Mock
    private StudentService studentService;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private CourseRepository courseRepository;

    private final UUID studentId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();

    private CommandDescriber describer() {
        return new CommandDescriber(studentRepository, bookRepository, courseRepository);
    }

    private void studentIs(StudentStatus status) {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(
                Student.builder().id(studentId).name("Ana").status(status).build()));
    }

    // --- CommandDescriber ---

    @Test
    void describer_usesNames() {
        studentIs(StudentStatus.ACTIVE);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(Book.builder().title("Clean Code").build()));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(
                Course.builder().courseName("Algorithms").code("CS101").build()));
        CommandDescriber describer = describer();

        assertThat(describer.describe(new AssignBookCommand(studentService, studentId, bookId)))
                .isEqualTo("Assign \"Clean Code\" to Ana");
        assertThat(describer.describe(new RemoveBookCommand(studentService, studentId, bookId)))
                .isEqualTo("Remove \"Clean Code\" from Ana");
        assertThat(describer.describe(new EnrollInCourseCommand(studentService, studentId, courseId)))
                .isEqualTo("Enroll Ana in Algorithms (CS101)");
        assertThat(describer.describe(new DropCourseCommand(studentService, studentId, courseId)))
                .isEqualTo("Drop Ana from Algorithms (CS101)");
        assertThat(describer.describe(new UpdateStudentCommand(studentService, studentId, new Student())))
                .isEqualTo("Update profile of Ana");
        assertThat(describer.describe(new ChangeStudentStatusCommand(studentService, studentId, StudentTransition.SUSPEND)))
                .isEqualTo("Suspend Ana");
    }

    @Test
    void describer_fallsBackToIds_whenEntitiesAreGone() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        assertThat(describer().describe(new AssignBookCommand(studentService, studentId, bookId)))
                .isEqualTo("Assign book " + bookId + " to student " + studentId);
    }

    // --- RequiredPermissionVisitor ---

    @Test
    void requiredPermission_forExecute() {
        assertThat(executePermission(new AssignBookCommand(studentService, studentId, bookId))).isEqualTo(StudentPermission.BORROW_BOOKS);
        assertThat(executePermission(new RemoveBookCommand(studentService, studentId, bookId))).isEqualTo(StudentPermission.NONE);
        assertThat(executePermission(new EnrollInCourseCommand(studentService, studentId, courseId))).isEqualTo(StudentPermission.ENROLL);
        assertThat(executePermission(new DropCourseCommand(studentService, studentId, courseId))).isEqualTo(StudentPermission.NONE);
        assertThat(executePermission(new UpdateStudentCommand(studentService, studentId, new Student()))).isEqualTo(StudentPermission.EDIT_PROFILE);
        assertThat(executePermission(new ChangeStudentStatusCommand(studentService, studentId, StudentTransition.GRADUATE))).isEqualTo(StudentPermission.NONE);
    }

    @Test
    void requiredPermission_forUndo_isThatOfTheOppositeAction() {
        assertThat(undoPermission(new AssignBookCommand(studentService, studentId, bookId))).isEqualTo(StudentPermission.NONE);
        assertThat(undoPermission(new RemoveBookCommand(studentService, studentId, bookId))).isEqualTo(StudentPermission.BORROW_BOOKS);
        assertThat(undoPermission(new EnrollInCourseCommand(studentService, studentId, courseId))).isEqualTo(StudentPermission.NONE);
        assertThat(undoPermission(new DropCourseCommand(studentService, studentId, courseId))).isEqualTo(StudentPermission.ENROLL);
        assertThat(undoPermission(new UpdateStudentCommand(studentService, studentId, new Student()))).isEqualTo(StudentPermission.EDIT_PROFILE);
        assertThat(undoPermission(new ChangeStudentStatusCommand(studentService, studentId, StudentTransition.GRADUATE))).isEqualTo(StudentPermission.NONE);
    }

    private static StudentPermission executePermission(StudentCommand<?> command) {
        return command.accept(RequiredPermissionVisitor.FOR_EXECUTE);
    }

    private static StudentPermission undoPermission(StudentCommand<?> command) {
        return command.accept(RequiredPermissionVisitor.FOR_UNDO);
    }

    // --- StatePermissionUndoGuard ---

    @Test
    void undoGuard_refusesReassigningBookToSuspendedStudent() {
        studentIs(StudentStatus.SUSPENDED);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(Book.builder().title("Clean Code").build()));
        UndoGuard guard = new StatePermissionUndoGuard(studentRepository, describer());

        assertThatThrownBy(() -> guard.check(new RemoveBookCommand(studentService, studentId, bookId)))
                .isInstanceOf(InvalidStudentStateException.class)
                .hasMessage("Cannot undo \"Remove \"Clean Code\" from Ana\": student is SUSPENDED");
    }

    @Test
    void undoGuard_allowsUndo_whenStateGrantsPermission() {
        studentIs(StudentStatus.ACTIVE);
        UndoGuard guard = new StatePermissionUndoGuard(studentRepository, describer());

        assertThatCode(() -> guard.check(new DropCourseCommand(studentService, studentId, courseId)))
                .doesNotThrowAnyException();
    }

    @Test
    void undoGuard_skipsLookup_whenNoPermissionNeeded() {
        UndoGuard guard = new StatePermissionUndoGuard(studentRepository, describer());

        assertThatCode(() -> guard.check(new AssignBookCommand(studentService, studentId, bookId)))
                .doesNotThrowAnyException();
        verifyNoInteractions(studentRepository);
    }

    @Test
    void undoGuard_letsUndoThrough_whenStudentWasDeleted() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());
        UndoGuard guard = new StatePermissionUndoGuard(studentRepository, describer());

        assertThatCode(() -> guard.check(new UpdateStudentCommand(studentService, studentId, new Student())))
                .doesNotThrowAnyException();
    }
}
