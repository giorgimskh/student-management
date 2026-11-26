package com.example.student_management.storage;

import com.example.student_management.domain.Student;
import com.example.student_management.domain.StudentPhoto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The adapter from {@link PhotoStorage} to the file system, on a temporary folder. */
class FileSystemPhotoStorageTest {
    private static final Instant FIRST_UPLOAD = Instant.parse("2026-01-01T00:00:00.123456789Z");
    private static final Instant SECOND_UPLOAD = Instant.parse("2026-02-01T00:00:00Z");

    @TempDir
    private Path root;

    private final Student student = Student.builder().id(UUID.randomUUID()).name("Nino").build();

    private FileSystemPhotoStorage storage() {
        return new FileSystemPhotoStorage(root);
    }

    private Path folder() {
        return root.resolve(student.getId().toString());
    }

    @Test
    void save_thenFind_roundTripsEveryField() {
        PhotoStorage storage = storage();

        StudentPhoto saved = storage.save(student, "ნინო.png", "image/png", new byte[]{1, 2, 3, 4}, FIRST_UPLOAD);
        StudentPhoto loaded = storage().findByStudentId(student.getId()).orElseThrow(); // a fresh instance reads the files

        assertThat(saved.getStudent()).isSameAs(student);
        assertThat(loaded.getData()).containsExactly(1, 2, 3, 4);
        assertThat(loaded.getContentType()).isEqualTo("image/png");
        assertThat(loaded.getOriginalFilename()).isEqualTo("ნინო.png");
        assertThat(loaded.getSizeBytes()).isEqualTo(4);
        assertThat(loaded.getUploadedAt()).isEqualTo(FIRST_UPLOAD);
    }

    @Test
    void save_writesTwoFilesInTheStudentsFolder_andNoTemporaryFiles() throws IOException {
        storage().save(student, "me.png", "image/png", new byte[]{7}, FIRST_UPLOAD);

        try (var files = Files.list(folder())) {
            assertThat(files.map(f -> f.getFileName().toString()))
                    .containsExactlyInAnyOrder(FileSystemPhotoStorage.DATA_FILE, FileSystemPhotoStorage.METADATA_FILE);
        }
        assertThat(Files.readAllBytes(folder().resolve(FileSystemPhotoStorage.DATA_FILE))).containsExactly(7);
    }

    @Test
    void secondSave_replacesTheFirstPhoto() {
        PhotoStorage storage = storage();
        storage.save(student, "old.png", "image/png", new byte[]{1}, FIRST_UPLOAD);

        storage.save(student, null, "image/jpeg", new byte[]{9, 9}, SECOND_UPLOAD);

        StudentPhoto loaded = storage.findByStudentId(student.getId()).orElseThrow();
        assertThat(loaded.getData()).containsExactly(9, 9);
        assertThat(loaded.getContentType()).isEqualTo("image/jpeg");
        assertThat(loaded.getOriginalFilename()).isNull();
        assertThat(loaded.getUploadedAt()).isEqualTo(SECOND_UPLOAD);
    }

    @Test
    void find_isEmpty_withoutAPhoto_orWithHalfOfOne() throws IOException {
        assertThat(storage().findByStudentId(student.getId())).isEmpty();

        Files.createDirectories(folder());
        Files.write(folder().resolve(FileSystemPhotoStorage.DATA_FILE), new byte[]{1}); // metadata never written
        assertThat(storage().findByStudentId(student.getId())).isEmpty();
    }

    @Test
    void delete_removesThePhotoAndItsFolder_andIsANoOpWhenThereIsNone() {
        PhotoStorage storage = storage();
        storage.save(student, "me.png", "image/png", new byte[]{1}, FIRST_UPLOAD);

        storage.deleteByStudentId(student.getId());
        storage.deleteByStudentId(student.getId());
        storage.deleteByStudentId(UUID.randomUUID());

        assertThat(storage.findByStudentId(student.getId())).isEmpty();
        assertThat(folder()).doesNotExist();
    }

    @Test
    void photosOfDifferentStudents_areKeptApart() {
        Student other = Student.builder().id(UUID.randomUUID()).build();
        PhotoStorage storage = storage();
        storage.save(student, "a.png", "image/png", new byte[]{1}, FIRST_UPLOAD);
        storage.save(other, "b.png", "image/png", new byte[]{2}, FIRST_UPLOAD);

        storage.deleteByStudentId(other.getId());

        assertThat(storage.findByStudentId(student.getId()).orElseThrow().getData()).containsExactly(1);
        assertThat(storage.findByStudentId(other.getId())).isEmpty();
    }

    @Test
    void createsTheRootFolderWhenMissing() {
        FileSystemPhotoStorage storage = new FileSystemPhotoStorage(root.resolve("not/there/yet"));

        storage.save(student, "me.png", "image/png", new byte[]{1}, FIRST_UPLOAD);

        assertThat(storage.findByStudentId(student.getId())).isPresent();
    }
}
