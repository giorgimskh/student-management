package com.example.student_management.domain;

import com.example.student_management.visitor.StudentGraphVisitor;
import com.example.student_management.visitor.Visitable;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name="courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course implements Visitable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String courseName;

    private String code;

    @ManyToMany(mappedBy ="courses", fetch=FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<Student> students=new HashSet<>();

    /** "Name (CODE)" for display, or whichever of the two is set; the id if neither is. */
    public String label() {
        boolean hasName = courseName != null && !courseName.isBlank();
        boolean hasCode = code != null && !code.isBlank();
        if (hasName && hasCode) return courseName + " (" + code + ")";
        if (hasName) return courseName;
        if (hasCode) return code;
        return "course " + id;
    }

    /** Visits only this element; it does not walk back to its student(s). */
    @Override
    public void accept(StudentGraphVisitor visitor) {
        visitor.visit(this);
    }
}
