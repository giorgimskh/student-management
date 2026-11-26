package com.example.student_management.search;

import com.example.student_management.exceptions.InvalidSearchException;
import com.example.student_management.state.StudentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

/** The builder itself. The queries it produces are checked against Postgres in StudentSearchRepositoryTest. */
class StudentSearchTest {
    private static final Instant JAN_1 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant FEB_1 = Instant.parse("2026-02-01T00:00:00Z");

    @Test
    void nothingSet_isUnrestricted() {
        assertThat(StudentSearch.builder().build().isUnrestricted()).isTrue();
        assertThat(StudentSearch.everyone().isUnrestricted()).isTrue();
    }

    @Test
    void nullsAndBlanks_areIgnored_soRequestParametersCanBePassedStraightThrough() {
        StudentSearch search = StudentSearch.builder()
                .nameContains("   ")
                .emailContains(null)
                .status((StudentStatus) null)
                .statuses(null)
                .enrolledIn(null)
                .withOverdueLoans(false)
                .createdFrom(null)
                .createdBefore(null)
                .build();

        assertThat(search.isUnrestricted()).isTrue();
    }

    @Test
    void keepsEveryPartThatWasSet_withTextTrimmed() {
        UUID course = UUID.randomUUID();

        StudentSearch search = StudentSearch.builder()
                .nameContains("  Ana ")
                .emailContains("example.com")
                .status(StudentStatus.ACTIVE)
                .enrolledIn(course)
                .withOverdueLoans()
                .createdFrom(JAN_1)
                .createdBefore(FEB_1)
                .build();

        assertThat(search.isUnrestricted()).isFalse();
        assertThat(search.nameContains()).isEqualTo("Ana");
        assertThat(search.emailContains()).isEqualTo("example.com");
        assertThat(search.statuses()).containsExactly(StudentStatus.ACTIVE);
        assertThat(search.courseId()).isEqualTo(course);
        assertThat(search.overdueOnly()).isTrue();
        assertThat(search.createdFrom()).isEqualTo(JAN_1);
        assertThat(search.createdBefore()).isEqualTo(FEB_1);
    }

    @Test
    void statuses_accumulate_andDuplicatesCollapse() {
        StudentSearch search = StudentSearch.builder()
                .status(StudentStatus.ACTIVE)
                .statuses(Arrays.asList(StudentStatus.SUSPENDED, null, StudentStatus.ACTIVE))
                .build();

        assertThat(search.statuses()).containsExactlyInAnyOrder(StudentStatus.ACTIVE, StudentStatus.SUSPENDED);
    }

    @Test
    void builtSearch_isNotChangedByLaterUseOfTheBuilder() {
        StudentSearch.Builder builder = StudentSearch.builder().status(StudentStatus.ACTIVE);
        StudentSearch first = builder.build();

        builder.status(StudentStatus.GRADUATED).nameContains("x");

        assertThat(first.statuses()).containsExactly(StudentStatus.ACTIVE);
        assertThat(first.nameContains()).isNull();
        assertThat(builder.build().statuses()).hasSize(2);
    }

    @Test
    void statuses_ofABuiltSearch_cannotBeModified() {
        StudentSearch search = StudentSearch.builder().status(StudentStatus.ACTIVE).build();

        assertThatThrownBy(() -> search.statuses().add(StudentStatus.WITHDRAWN))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void emptyDateRange_isRejectedOnBuild() {
        assertThatThrownBy(() -> StudentSearch.builder().createdFrom(FEB_1).createdBefore(JAN_1).build())
                .isInstanceOf(InvalidSearchException.class);
        assertThatThrownBy(() -> StudentSearch.builder().createdFrom(JAN_1).createdBefore(JAN_1).build())
                .isInstanceOf(InvalidSearchException.class);
        assertThatNoException().isThrownBy(() -> StudentSearch.builder().createdFrom(JAN_1).build());
    }

    @Test
    void likePattern_isLowerCase_andEscapesWildcards() {
        assertThat(StudentSearch.likePattern("Ana")).isEqualTo("%ana%");
        assertThat(StudentSearch.likePattern("100%_done\\")).isEqualTo("%100\\%\\_done\\\\%");
    }

    @Test
    void overdueSearch_needsTheCurrentTime() {
        StudentSearch search = StudentSearch.builder().withOverdueLoans().build();

        assertThatThrownBy(() -> search.toSpecification(null)).isInstanceOf(NullPointerException.class);
        assertThat(search.toSpecification(JAN_1)).isNotNull();
        assertThatNoException().isThrownBy(() -> StudentSearch.everyone().toSpecification(null)); // no time needed without it
    }
}
