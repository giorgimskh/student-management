package com.example.student_management.repository;

import com.example.student_management.domain.Student;
import com.example.student_management.state.StudentStatus;
import com.example.student_management.state.StudentTransition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@DataJpaTest
class StudentStatusPersistenceTest {
    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void statusIsStoredByNameAndReadBack() {
        Student student = Student.builder().name("Nino").email("nino@example.com").build();
        student.apply(StudentTransition.SUSPEND);
        UUID id = entityManager.persistFlushFind(student).getId();
        entityManager.clear();

        assertThat(studentRepository.findById(id).orElseThrow().getStatus()).isEqualTo(StudentStatus.SUSPENDED);
        Object stored = entityManager.getEntityManager()
                .createNativeQuery("select status from students where id = :id")
                .setParameter("id", id)
                .getSingleResult();
        assertThat(stored).isEqualTo("SUSPENDED");
    }

    @Test
    void rowWithoutStatus_isReadAsActive() {
        UUID id = entityManager.persistFlushFind(
                Student.builder().name("Old").email("old@example.com").build()).getId();
        entityManager.getEntityManager()
                .createNativeQuery("update students set status = null where id = :id")
                .setParameter("id", id)
                .executeUpdate();
        entityManager.clear();

        Student loaded = studentRepository.findById(id).orElseThrow();

        assertThat(loaded.getStatus()).isEqualTo(StudentStatus.ACTIVE);
        assertThat(loaded.getState().canEnroll()).isTrue();
    }
}
