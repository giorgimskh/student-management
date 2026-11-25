package com.example.student_management.domain;

import com.example.student_management.visitor.StudentGraphVisitor;
import com.example.student_management.visitor.Visitable;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="books")
@Builder
public class Book implements Visitable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;

    private String isbn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Student student;

    /** Visits only this element; it does not walk back to its student(s). */
    @Override
    public void accept(StudentGraphVisitor visitor) {
        visitor.visit(this);
    }
}
