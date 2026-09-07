package com.example.student_management.domain;

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
public class StudentPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", unique = true, nullable = false)
    private Student student;

    @Lob
    @Column(columnDefinition = "bytea", nullable = false)
    private byte[] data;

    @Column(nullable = false)
    private String contentType;

    private String originalFilename;

    @Column(nullable = false)
    private long sizeBytes;

    private Instant uploadedAt;
}