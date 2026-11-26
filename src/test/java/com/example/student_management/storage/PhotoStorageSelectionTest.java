package com.example.student_management.storage;

import com.example.student_management.repository.StudentPhotoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** Exactly one PhotoStorage exists, picked with app.photos.storage. */
class PhotoStorageSelectionTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(StudentPhotoRepository.class, () -> mock(StudentPhotoRepository.class))
            .withUserConfiguration(DatabasePhotoStorage.class, FileSystemPhotoStorage.class);

    @Test
    void defaultsToTheDatabase() {
        runner.run(context -> assertThat(context).hasSingleBean(PhotoStorage.class)
                .getBean(PhotoStorage.class).isInstanceOf(DatabasePhotoStorage.class));
    }

    @Test
    void fileSystemWhenConfigured_inTheConfiguredFolder(@TempDir Path folder) {
        runner.withPropertyValues("app.photos.storage=filesystem", "app.photos.directory=" + folder)
                .run(context -> {
                    assertThat(context).hasSingleBean(PhotoStorage.class);
                    assertThat(context.getBean(PhotoStorage.class)).isInstanceOfSatisfying(FileSystemPhotoStorage.class,
                            storage -> assertThat(storage.root()).isEqualTo(folder.toAbsolutePath().normalize()));
                });
    }
}
