package com.example.student_management.storage;

import com.example.student_management.domain.Student;
import com.example.student_management.domain.StudentPhoto;
import com.example.student_management.repository.StudentPhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import com.example.student_management.TestcontainersConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // use the Testcontainers Postgres
@DataJpaTest
@Import({DatabasePhotoStorage.class, TestcontainersConfiguration.class})
class DatabasePhotoStorageTest {
    private static final Instant FIRST_UPLOAD = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant SECOND_UPLOAD = Instant.parse("2026-02-01T00:00:00Z");

    @Autowired
    private PhotoStorage photoStorage;

    @Autowired
    private StudentPhotoRepository studentPhotoRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Student student;

    @BeforeEach
    void setUp() {
        student = entityManager.persistFlushFind(
                Student.builder().name("Nino").email("nino@example.com").build());
    }

    @Test
    void save_createsPhotoWithAllFields() {
        byte[] data = {1, 2, 3, 4};

        StudentPhoto saved = photoStorage.save(student, "me.png", "image/png", data, FIRST_UPLOAD);
        entityManager.flush();
        entityManager.clear();

        StudentPhoto loaded = photoStorage.findByStudentId(student.getId()).orElseThrow();
        assertThat(loaded.getId()).isEqualTo(saved.getId());
        assertThat(loaded.getOriginalFilename()).isEqualTo("me.png");
        assertThat(loaded.getContentType()).isEqualTo("image/png");
        assertThat(loaded.getData()).containsExactly(1, 2, 3, 4);
        assertThat(loaded.getSizeBytes()).isEqualTo(4);
        assertThat(loaded.getUploadedAt()).isEqualTo(FIRST_UPLOAD); // exact round trip on real Postgres
    }

    @Test
    void save_twiceForSameStudent_replacesTheExistingRow() {
        StudentPhoto first = photoStorage.save(student, "old.png", "image/png", new byte[]{1}, FIRST_UPLOAD);
        entityManager.flush();

        StudentPhoto second = photoStorage.save(student, "new.jpg", "image/jpeg", new byte[]{9, 9}, SECOND_UPLOAD);
        entityManager.flush();
        entityManager.clear();

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(studentPhotoRepository.count()).isEqualTo(1);
        StudentPhoto loaded = photoStorage.findByStudentId(student.getId()).orElseThrow();
        assertThat(loaded.getOriginalFilename()).isEqualTo("new.jpg");
        assertThat(loaded.getContentType()).isEqualTo("image/jpeg");
        assertThat(loaded.getSizeBytes()).isEqualTo(2);
        assertThat(loaded.getUploadedAt()).isEqualTo(SECOND_UPLOAD);
    }

    @Test
    void findByStudentId_isEmptyForStudentWithoutPhoto() {
        assertThat(photoStorage.findByStudentId(student.getId())).isEmpty();
        assertThat(photoStorage.findByStudentId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void deleteByStudentId_removesThePhoto() {
        photoStorage.save(student, "me.png", "image/png", new byte[]{1}, FIRST_UPLOAD);
        entityManager.flush();

        photoStorage.deleteByStudentId(student.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(photoStorage.findByStudentId(student.getId())).isEmpty();
        assertThat(studentPhotoRepository.count()).isZero();
    }

    @Test
    void deleteByStudentId_withoutPhoto_isANoOp() {
        photoStorage.deleteByStudentId(student.getId());
        photoStorage.deleteByStudentId(UUID.randomUUID());

        assertThat(studentPhotoRepository.count()).isZero();
    }
}
