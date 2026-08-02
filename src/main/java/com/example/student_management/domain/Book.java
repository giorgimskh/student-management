package com.example.student_management.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="books")

public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String title;
    private String isbn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "student_id",         // Customizes the foreign key column name
            referencedColumnName = "id", // Points to the primary key of the parent
            nullable = false,            // Makes the foreign key NOT NULL
            unique = false
    )
    private Student student;
}
