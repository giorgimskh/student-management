package com.example.student_management.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

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

    @NotBlank(message="Book Title is required")
    private String title;

    @NotBlank(message = "ISBN cannot be blank")
    @Pattern(regexp = "^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|97[89][0-9]{10}$|(?=(?:[0-9]+[-]-?){3}[0-9X]{1}$|97[89](?:-[0-9]+){3}[0-9]{1}$))[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$"
            ,message = "ISBN must be 10 or 13 digits")
    private String isbn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "student_id",         // Customizes the foreign key column name
            referencedColumnName = "id", // Points to the primary key of the parent
            nullable = true,
            unique = false
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Student student;
}
