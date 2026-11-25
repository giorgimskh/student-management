package com.example.student_management.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Upgrading a database that Hibernate's ddl-auto created before Flyway existed:
 * it has the tables but no Flyway history. Flyway must baseline it at version 1
 * (skipping V1) and run only V2 and V3, keeping the data.
 */
@Testcontainers
class FlywayUpgradeTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    private final UUID studentId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();

    @BeforeEach
    void freshDatabase() throws SQLException {
        sql("drop schema public cascade", "create schema public");
    }

    @AfterEach
    void cleanUp() throws SQLException {
        sql("drop schema public cascade", "create schema public");
    }

    private void sql(String... statements) throws SQLException {
        try (Connection c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement s = c.createStatement()) {
            for (String statement : statements) {
                s.execute(statement);
            }
        }
    }

    private String query(String sql) throws SQLException {
        try (Connection c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    /** Same settings as application.properties. */
    private Flyway flyway() {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load();
    }

    /** The tables ddl-auto made before Flyway, with a student who has no status and a borrowed book. */
    private void legacySchemaWithData(boolean withStatusColumn) throws SQLException {
        sql("create table students (id uuid primary key, created_at timestamptz, email varchar(255), name varchar(255)"
                        + (withStatusColumn ? ", status varchar(20)" : "") + ")",
                "create table books (id uuid primary key, isbn varchar(255), title varchar(255), student_id uuid references students)",
                "create table courses (id uuid primary key, code varchar(255), course_name varchar(255))",
                "create table student_courses (student_id uuid not null references students, course_id uuid not null references courses, primary key (student_id, course_id))",
                "create table student_photos (id uuid primary key, content_type varchar(255) not null, data bytea not null, original_filename varchar(255),"
                        + " size_bytes bigint not null, uploaded_at timestamptz, student_id uuid not null unique references students)",
                "insert into students (id, name, email) values ('" + studentId + "', 'Old Student', 'old@example.com')",
                "insert into books (id, title, isbn, student_id) values ('" + bookId + "', 'Old Book', '9780134757599', '" + studentId + "')");
    }

    @Test
    void existingDatabase_isBaselined_thenUpgraded_keepingData() throws SQLException {
        legacySchemaWithData(true);

        flyway().migrate();

        assertThat(Arrays.stream(flyway().info().applied()).map(MigrationInfo::getVersion).map(Object::toString))
                .containsExactly("1", "2", "3"); // 1 is the baseline marker, V1 itself did not run
        assertThat(query("select status from students where id = '" + studentId + "'")).isEqualTo("ACTIVE");
        assertThat(query("select is_nullable from information_schema.columns where table_name = 'students' and column_name = 'status'"))
                .isEqualTo("NO");
        assertThat(query("select count(*) from book_loans where book_id = '" + bookId + "' and student_id = '" + studentId
                + "' and returned_at is null and due_at > borrowed_at")).isEqualTo("1");
    }

    @Test
    void databaseFromBeforeTheStatusColumn_getsTheColumnToo() throws SQLException {
        legacySchemaWithData(false);

        flyway().migrate();

        assertThat(query("select status from students where id = '" + studentId + "'")).isEqualTo("ACTIVE");
    }

    @Test
    void emptyDatabase_runsAllMigrations() throws SQLException {
        flyway().migrate();

        assertThat(Arrays.stream(flyway().info().applied()).map(MigrationInfo::getDescription))
                .containsExactly("initial schema", "student status required", "book loans");
        assertThat(query("select count(*) from information_schema.tables where table_name = 'book_loans'")).isEqualTo("1");
    }
}
