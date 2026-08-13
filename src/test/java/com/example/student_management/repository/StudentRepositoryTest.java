package com.example.student_management.repository;

import com.example.student_management.domain.Student;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@DataJpaTest
public class StudentRepositoryTest {
    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TestEntityManager entityManager;


    @Test
    public void StudentRepository_SaveAll_ReturnSavedStudent(){
        //arrange
        Student student = Student.builder().name("John").email("john@email.com").build();

        //act
        Student savedStudent = studentRepository.save(student);

        //assert
        Assertions.assertThat(savedStudent).isNotNull();
        Assertions.assertThat(savedStudent.getId()).isNotNull();
        Assertions.assertThat(savedStudent.getName()).isEqualTo("John");
        Assertions.assertThat(savedStudent.getEmail()).isEqualTo("john@email.com");
    }

    @Test
    public void findByEmail_returnStudent_whenEmailExists(){
        Student student = Student.builder()
                .name("Nino Kapanadze")
                .email("nino@example.com")
                .build();

        entityManager.persistAndFlush(student);

        Optional<Student> result=studentRepository.findByEmail(student.getEmail());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo(student.getName());

        assertThat(result.get().getId()).isEqualTo(student.getId());
    }

    @Test
    public void findByEmail_returnsEmpty_whenEmailDoesNotExist(){
        Student student = Student.builder()
                .name("Nino Kapanadze")
                .email("nino@example.com")
                .build();

        entityManager.persistAndFlush(student);

        Optional<Student> result = studentRepository.findByEmail("no@example.com");
        assertThat(result).isEmpty();
    }
}
