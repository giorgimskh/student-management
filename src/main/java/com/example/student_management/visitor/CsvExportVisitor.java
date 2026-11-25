package com.example.student_management.visitor;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import com.example.student_management.domain.StudentPhoto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exports one CSV row per student: name, email, status, the titles of their
 * books, their course codes (name if no code) and whether they have a photo.
 * List cells are joined with {@code "; "} and sorted, so the output is stable.
 */
public class CsvExportVisitor implements StudentGraphVisitor {
    static final String HEADER = "name,email,status,books,courses,has_photo";

    private final List<Row> rows = new ArrayList<>();
    private Row current;

    @Override
    public void visit(Student student) {
        current = new Row(student.getName(), student.getEmail(), student.getStatus().name());
        rows.add(current);
    }

    @Override
    public void visit(Book book) {
        current.books.add(book.getTitle());
    }

    @Override
    public void visit(Course course) {
        String code = course.getCode();
        current.courses.add(code == null || code.isBlank() ? course.label() : code);
    }

    @Override
    public void visit(StudentPhoto photo) {
        current.hasPhoto = true;
    }

    /** The CSV text, header first, lines ending in CRLF (RFC 4180). */
    public String result() {
        StringBuilder csv = new StringBuilder(HEADER).append("\r\n");
        for (Row row : rows) {
            csv.append(String.join(",",
                    cell(row.name),
                    cell(row.email),
                    cell(row.status),
                    cell(joined(row.books)),
                    cell(joined(row.courses)),
                    String.valueOf(row.hasPhoto)
            )).append("\r\n");
        }
        return csv.toString();
    }

    private static String joined(List<String> values) {
        return values.stream().map(String::valueOf).sorted().collect(Collectors.joining("; "));
    }

    /**
     * Quotes a cell when needed. Cells starting with a formula character get a
     * leading {@code '} so spreadsheets do not run them (CSV injection).
     */
    static String cell(String value) {
        if (value == null) {
            return "";
        }
        if (!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0) {
            value = "'" + value;
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static final class Row {
        private final String name;
        private final String email;
        private final String status;
        private final List<String> books = new ArrayList<>();
        private final List<String> courses = new ArrayList<>();
        private boolean hasPhoto;

        private Row(String name, String email, String status) {
            this.name = name;
            this.email = email;
            this.status = status;
        }
    }
}
