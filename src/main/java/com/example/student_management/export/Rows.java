package com.example.student_management.export;

import java.util.List;

/** Checks shared by every format's writers. */
final class Rows {
    private Rows() {
    }

    static void requireColumns(List<?> cells, int columns) {
        if (cells.size() != columns) {
            throw new IllegalArgumentException("Row has " + cells.size() + " cells but there are " + columns + " columns");
        }
    }
}
