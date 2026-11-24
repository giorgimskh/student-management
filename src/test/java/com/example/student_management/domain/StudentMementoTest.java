package com.example.student_management.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StudentMementoTest {

    @Test
    void restore_bringsBackNameAndEmail_afterChanges() {
        Student student = Student.builder().name("Nino").email("nino@example.com").build();
        StudentMemento snapshot = student.createMemento();

        student.setName("Changed");
        student.setEmail("changed@example.com");
        student.restore(snapshot);

        assertThat(student.getName()).isEqualTo("Nino");
        assertThat(student.getEmail()).isEqualTo("nino@example.com");
    }

    @Test
    void memento_isNotAffectedByLaterChanges() {
        Student student = Student.builder().name("Nino").email("nino@example.com").build();
        StudentMemento snapshot = student.createMemento();

        student.setName("Changed");

        assertThat(snapshot.name()).isEqualTo("Nino");
    }
}
