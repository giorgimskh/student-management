package com.example.student_management.domain;

import com.example.student_management.visitor.StudentGraphVisitor;
import com.example.student_management.visitor.Visitable;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_photos")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StudentPhoto implements Visitable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", unique = true, nullable = false)
    private Student student;

    // No @Lob: with PostgreSQLDialect, @Lob byte[] is bound as a large-object id
    // (bigint), which fails against this bytea column. A plain byte[] binds as bytea.
    @Column(columnDefinition = "bytea", nullable = false)
    private byte[] data;

    @Column(nullable = false)
    private String contentType;

    private String originalFilename;

    @Column(nullable = false)
    private long sizeBytes;

    private Instant uploadedAt;

    /** Visits only this element; it does not walk back to its student(s). */
    @Override
    public void accept(StudentGraphVisitor visitor) {
        visitor.visit(this);
    }
}
