package com.example.student_management.export;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.BookLoan;
import com.example.student_management.repository.BookLoanRepository;
import com.example.student_management.repository.BookRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Every book, with who has it and when it is due. */
@Component
public class BookCsvExporter extends CsvExporter<Book> {
    private final BookRepository bookRepository;
    private final BookLoanRepository loanRepository;
    private Map<UUID, BookLoan> openLoanByBook = Map.of();

    public BookCsvExporter(BookRepository bookRepository, BookLoanRepository loanRepository) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
    }

    @Override
    public String type() {
        return "books";
    }

    /** Uses the hook to load all open loans in one query instead of one per book. */
    @Override
    protected void prepare() {
        Map<UUID, BookLoan> byBook = new HashMap<>();
        loanRepository.findByReturnedAtIsNullOrderByDueAtAsc().forEach(loan -> byBook.put(loan.getBook().getId(), loan));
        openLoanByBook = byBook;
    }

    @Override
    protected List<String> headers() {
        return List.of("id", "title", "isbn", "borrowed_by", "due_at");
    }

    @Override
    protected List<Book> records() {
        return bookRepository.findAll(Sort.by("title"));
    }

    @Override
    protected List<?> toRow(Book book) {
        BookLoan loan = openLoanByBook.get(book.getId());
        return Arrays.asList(book.getId(), book.getTitle(), book.getIsbn(),
                loan == null ? null : loan.getStudent().getName(),
                loan == null ? null : loan.getDueAt());
    }
}
