package com.example.student_management.search;

import com.example.student_management.domain.BookLoan;
import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import com.example.student_management.exceptions.InvalidSearchException;
import com.example.student_management.state.StudentStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Builder: the criteria for finding students, assembled one optional part at a
 * time and then frozen by {@link Builder#build()}. Only the parts that were set
 * become conditions, so one query replaces a repository method per combination
 * of filters.
 *
 * <pre>{@code
 * StudentSearch.builder()
 *     .nameContains("ana")
 *     .status(StudentStatus.ACTIVE, StudentStatus.SUSPENDED)
 *     .enrolledIn(courseId)
 *     .withOverdueLoans()
 *     .build()
 *     .toSpecification(now);
 * }</pre>
 *
 * Every setter ignores null or blank values, so request parameters can be
 * passed straight through. Conditions are combined with AND; several statuses
 * mean "any of these".
 */
public final class StudentSearch {
    private final String nameContains;
    private final String emailContains;
    private final Set<StudentStatus> statuses;
    private final UUID courseId;
    private final boolean overdueOnly;
    private final Instant createdFrom;
    private final Instant createdBefore;

    private StudentSearch(Builder builder) {
        this.nameContains = builder.nameContains;
        this.emailContains = builder.emailContains;
        this.statuses = builder.statuses.isEmpty() ? Set.of() : Set.copyOf(builder.statuses);
        this.courseId = builder.courseId;
        this.overdueOnly = builder.overdueOnly;
        this.createdFrom = builder.createdFrom;
        this.createdBefore = builder.createdBefore;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** A search with no conditions: every student. */
    public static StudentSearch everyone() {
        return builder().build();
    }

    /** True if no condition was set. */
    public boolean isUnrestricted() {
        return nameContains == null && emailContains == null && statuses.isEmpty() && courseId == null
                && !overdueOnly && createdFrom == null && createdBefore == null;
    }

    /**
     * The JPA query for these criteria.
     *
     * @param now the moment a loan's due date is compared with for {@link Builder#withOverdueLoans()}
     */
    public Specification<Student> toSpecification(Instant now) {
        List<Specification<Student>> conditions = new ArrayList<>();
        if (nameContains != null) {
            conditions.add((root, query, cb) -> cb.like(cb.lower(root.get("name")), likePattern(nameContains), '\\'));
        }
        if (emailContains != null) {
            conditions.add((root, query, cb) -> cb.like(cb.lower(root.get("email")), likePattern(emailContains), '\\'));
        }
        if (!statuses.isEmpty()) {
            conditions.add((root, query, cb) -> root.get("status").in(statuses));
        }
        if (courseId != null) {
            // A subquery, not a join on the student, so a student is never listed twice and paging counts stay right.
            conditions.add((root, query, cb) -> {
                Subquery<UUID> enrolled = query.subquery(UUID.class);
                Root<Student> s = enrolled.from(Student.class);
                Join<Student, Course> course = s.join("courses");
                enrolled.select(s.get("id")).where(cb.equal(s, root), cb.equal(course.get("id"), courseId));
                return cb.exists(enrolled);
            });
        }
        if (overdueOnly) {
            Objects.requireNonNull(now, "now is needed to find overdue loans");
            conditions.add((root, query, cb) -> {
                Subquery<UUID> overdue = query.subquery(UUID.class);
                Root<BookLoan> loan = overdue.from(BookLoan.class);
                overdue.select(loan.get("id")).where(
                        cb.equal(loan.get("student"), root),
                        cb.isNull(loan.get("returnedAt")),
                        cb.lessThan(loan.get("dueAt"), now));
                return cb.exists(overdue);
            });
        }
        if (createdFrom != null) {
            conditions.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
        }
        if (createdBefore != null) {
            conditions.add((root, query, cb) -> cb.lessThan(root.get("createdAt"), createdBefore));
        }
        return conditions.isEmpty() ? Specification.unrestricted() : Specification.allOf(conditions);
    }

    /** Lower-cased {@code %text%}, with the LIKE wildcards in the text escaped so they match literally. */
    static String likePattern(String text) {
        String escaped = text.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    public String nameContains() {
        return nameContains;
    }

    public String emailContains() {
        return emailContains;
    }

    public Set<StudentStatus> statuses() {
        return statuses;
    }

    public UUID courseId() {
        return courseId;
    }

    public boolean overdueOnly() {
        return overdueOnly;
    }

    public Instant createdFrom() {
        return createdFrom;
    }

    public Instant createdBefore() {
        return createdBefore;
    }

    @Override
    public String toString() {
        return "StudentSearch[name~" + nameContains + ", email~" + emailContains + ", statuses=" + statuses
                + ", course=" + courseId + ", overdueOnly=" + overdueOnly
                + ", created=[" + createdFrom + ", " + createdBefore + ")]";
    }

    /** Collects the parts; each setter returns the builder so calls can be chained. Not thread-safe; use one per search. */
    public static final class Builder {
        private String nameContains;
        private String emailContains;
        private final Set<StudentStatus> statuses = EnumSet.noneOf(StudentStatus.class);
        private UUID courseId;
        private boolean overdueOnly;
        private Instant createdFrom;
        private Instant createdBefore;

        private Builder() {
        }

        /** Name contains this text, ignoring case. Surrounding spaces are trimmed; blank means no condition. */
        public Builder nameContains(String text) {
            this.nameContains = trimToNull(text);
            return this;
        }

        /** Email contains this text, ignoring case. Blank means no condition. */
        public Builder emailContains(String text) {
            this.emailContains = trimToNull(text);
            return this;
        }

        /** Status is any of these. Adds to statuses given earlier; nulls are ignored. */
        public Builder status(StudentStatus... statuses) {
            return statuses(statuses == null ? List.of() : Arrays.asList(statuses));
        }

        /** Status is any of these. Adds to statuses given earlier; null or empty means no condition. */
        public Builder statuses(Collection<StudentStatus> statuses) {
            if (statuses != null) {
                statuses.stream().filter(Objects::nonNull).forEach(this.statuses::add);
            }
            return this;
        }

        /** Enrolled in this course. Null means no condition. */
        public Builder enrolledIn(UUID courseId) {
            this.courseId = courseId;
            return this;
        }

        /** Has at least one book that is still out and past its due date. */
        public Builder withOverdueLoans() {
            return withOverdueLoans(true);
        }

        /** {@link #withOverdueLoans()} if {@code onlyOverdue}, no condition otherwise; handy for an optional flag. */
        public Builder withOverdueLoans(boolean onlyOverdue) {
            this.overdueOnly = onlyOverdue;
            return this;
        }

        /** Created at or after this moment. Null means no lower bound. */
        public Builder createdFrom(Instant from) {
            this.createdFrom = from;
            return this;
        }

        /** Created strictly before this moment. Null means no upper bound. */
        public Builder createdBefore(Instant before) {
            this.createdBefore = before;
            return this;
        }

        /**
         * Freezes the criteria. The builder can be changed and built again afterwards
         * without affecting searches already built.
         *
         * @throws InvalidSearchException if the creation range is empty (from is not before "before")
         */
        public StudentSearch build() {
            if (createdFrom != null && createdBefore != null && !createdFrom.isBefore(createdBefore)) {
                throw new InvalidSearchException("The creation date range is empty: 'from' must be before 'to'");
            }
            return new StudentSearch(this);
        }

        private static String trimToNull(String text) {
            return text == null || text.isBlank() ? null : text.trim();
        }
    }
}
