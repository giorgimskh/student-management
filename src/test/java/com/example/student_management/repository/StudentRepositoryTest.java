package com.example.student_management.repository;

import com.example.student_management.domain.Student;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;


@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@DataJpaTest
public class StudentRepositoryTest {
    @Autowired
    private StudentRepository studentRepository;

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
}
