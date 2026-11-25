package com.example.student_management.repository;

import com.example.student_management.TestcontainersConfiguration;
import com.example.student_management.domain.Student;
import com.example.student_management.state.StudentStatus;
import com.example.student_management.state.StudentTransition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class StudentStatusPersistenceTest {
    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID persist(String name, StudentStatus status) {
        return entityManager.persistFlushFind(Student.builder()
                .name(name).email(name.toLowerCase() + "@example.com").status(status).build()).getId();
    }

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
    void statusColumnIsRequired() {
        UUID id = persist("Old", StudentStatus.ACTIVE);

        assertThatThrownBy(() -> entityManager.getEntityManager()
                .createNativeQuery("update students set status = null where id = :id")
                .setParameter("id", id)
                .executeUpdate())
                .hasMessageContaining("null value in column \"status\"");
    }

    @Test
    void findByStatus_andByNameAndStatus() {
        persist("Ana", StudentStatus.ACTIVE);
        persist("Anano", StudentStatus.SUSPENDED);
        persist("Beka", StudentStatus.SUSPENDED);
        PageRequest byName = PageRequest.of(0, 10, Sort.by("name"));

        assertThat(studentRepository.findByStatus(StudentStatus.SUSPENDED, byName))
                .extracting(Student::getName).containsExactly("Anano", "Beka");
        assertThat(studentRepository.findByNameContainingIgnoreCaseAndStatus("ana", StudentStatus.SUSPENDED, byName))
                .extracting(Student::getName).containsExactly("Anano");
        assertThat(studentRepository.findByNameContainingIgnoreCaseAndStatus("ana", StudentStatus.GRADUATED, byName))
                .isEmpty();
    }
}
