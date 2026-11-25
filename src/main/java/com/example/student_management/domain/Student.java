package com.example.student_management.domain;

import com.example.student_management.state.StudentState;
import com.example.student_management.state.StudentStatus;
import com.example.student_management.state.StudentTransition;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Builder
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private String email;

    private Instant createdAt;

    /** Required in the database (migration V2 filled in ACTIVE for older rows). */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private StudentStatus status = StudentStatus.ACTIVE;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Book> books = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "student_courses",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<Course> courses = new HashSet<>();

    @OneToOne(mappedBy = "student",cascade = CascadeType.ALL,orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private StudentPhoto studentPhoto;

    /** ACTIVE if not set yet (e.g. {@code setStatus(null)} or {@code new Student()} before saving). */
    public StudentStatus getStatus() {
        return status == null ? StudentStatus.ACTIVE : status;
    }

    /** The behaviour for the current status (State pattern). */
    public StudentState getState() {
        return getStatus().state();
    }

    /** Moves to the status {@code transition} leads to, or throws if the current state forbids it. */
    public void apply(StudentTransition transition) {
        this.status = transition.applyTo(getState()).status();
    }

    /** Snapshot of the profile fields an update can change (name and email). */
    public StudentMemento createMemento() {
        return new StudentMemento(name, email);
    }

    /** Puts the profile fields back to the state captured in {@code memento}. */
    public void restore(StudentMemento memento) {
        this.name = memento.name();
        this.email = memento.email();
    }
}