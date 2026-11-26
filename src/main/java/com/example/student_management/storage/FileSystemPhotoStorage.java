package com.example.student_management.storage;

import com.example.student_management.domain.Student;
import com.example.student_management.domain.StudentPhoto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * Adapter: makes the file system ({@link Files}, the adaptee) usable wherever
 * the application expects a {@link PhotoStorage} (the target). The photo
 * service keeps calling save/find/delete and never learns that the photo is
 * now two files instead of a table row:
 *
 * <pre>
 * {root}/{studentId}/photo.bin         the image bytes
 * {root}/{studentId}/photo.properties  content type, original file name, size, upload time
 * </pre>
 *
 * Chosen with {@code app.photos.storage=filesystem}; the folder is
 * {@code app.photos.directory}. Each file is written to a temporary file and
 * then moved into place, so a reader never sees half a photo. The metadata is
 * written last and a photo only counts as present when both files exist.
 *
 * <p>Differences from {@link DatabasePhotoStorage}: the files are not part of
 * the database transaction, a loaded photo has no id and no student attached
 * (the caller already knows the student), and {@code Student.getStudentPhoto()}
 * stays empty, so statistics and audit reports do not see these photos.
 */
@Component
@ConditionalOnProperty(name = "app.photos.storage", havingValue = "filesystem")
public class FileSystemPhotoStorage implements PhotoStorage {
    static final String DATA_FILE = "photo.bin";
    static final String METADATA_FILE = "photo.properties";

    private static final String CONTENT_TYPE = "contentType";
    private static final String ORIGINAL_FILENAME = "originalFilename";
    private static final String SIZE_BYTES = "sizeBytes";
    private static final String UPLOADED_AT = "uploadedAt";

    private final Path root;

    @Autowired
    public FileSystemPhotoStorage(
            @Value("${app.photos.directory:${user.home}/.student-management/photos}") String directory) {
        this(Path.of(directory));
    }

    FileSystemPhotoStorage(Path root) {
        this.root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
    }

    /** The folder photos are kept in. */
    public Path root() {
        return root;
    }

    @Override
    public StudentPhoto save(Student student, String originalFilename, String contentType, byte[] data, Instant uploadedAt) {
        Path folder = folderFor(student.getId());
        Properties metadata = new Properties();
        metadata.setProperty(CONTENT_TYPE, contentType);
        if (originalFilename != null) {
            metadata.setProperty(ORIGINAL_FILENAME, originalFilename);
        }
        metadata.setProperty(SIZE_BYTES, Long.toString(data.length));
        if (uploadedAt != null) {
            metadata.setProperty(UPLOADED_AT, uploadedAt.toString());
        }

        try {
            Files.createDirectories(folder);
            replace(folder, DATA_FILE, temp -> Files.write(temp, data));
            replace(folder, METADATA_FILE, temp -> {
                try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                    metadata.store(writer, null);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store the photo of student " + student.getId(), e);
        }

        return StudentPhoto.builder()
                .student(student)
                .data(data)
                .contentType(contentType)
                .originalFilename(originalFilename)
                .sizeBytes(data.length)
                .uploadedAt(uploadedAt)
                .build();
    }

    @Override
    public Optional<StudentPhoto> findByStudentId(UUID studentId) {
        Path folder = folderFor(studentId);
        Path dataFile = folder.resolve(DATA_FILE);
        Path metadataFile = folder.resolve(METADATA_FILE);
        if (!Files.isRegularFile(dataFile) || !Files.isRegularFile(metadataFile)) {
            return Optional.empty();
        }

        try {
            Properties metadata = new Properties();
            try (Reader reader = Files.newBufferedReader(metadataFile, StandardCharsets.UTF_8)) {
                metadata.load(reader);
            }
            byte[] data = Files.readAllBytes(dataFile);
            String uploadedAt = metadata.getProperty(UPLOADED_AT);
            return Optional.of(StudentPhoto.builder()
                    .data(data)
                    .contentType(metadata.getProperty(CONTENT_TYPE))
                    .originalFilename(metadata.getProperty(ORIGINAL_FILENAME))
                    .sizeBytes(Long.parseLong(metadata.getProperty(SIZE_BYTES, Long.toString(data.length))))
                    .uploadedAt(uploadedAt == null ? null : Instant.parse(uploadedAt))
                    .build());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the photo of student " + studentId, e);
        }
    }

    @Override
    public void deleteByStudentId(UUID studentId) {
        Path folder = folderFor(studentId);
        try {
            // Metadata first: without it the photo already counts as gone.
            Files.deleteIfExists(folder.resolve(METADATA_FILE));
            Files.deleteIfExists(folder.resolve(DATA_FILE));
            Files.deleteIfExists(folder);
        } catch (DirectoryNotEmptyException e) {
            // A leftover temporary file from an interrupted save; the photo itself is gone.
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete the photo of student " + studentId, e);
        }
    }

    /** A UUID's text form has only hex digits and dashes, so it cannot climb out of the root. */
    private Path folderFor(UUID studentId) {
        return root.resolve(Objects.requireNonNull(studentId, "studentId").toString());
    }

    /** Writes a temporary file next to the target and moves it over the target. */
    private static void replace(Path folder, String fileName, FileWriter writer) throws IOException {
        Path temp = Files.createTempFile(folder, fileName, ".tmp");
        try {
            writer.write(temp);
            try {
                Files.move(temp, folder.resolve(fileName), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, folder.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    @FunctionalInterface
    private interface FileWriter {
        void write(Path file) throws IOException;
    }
}
