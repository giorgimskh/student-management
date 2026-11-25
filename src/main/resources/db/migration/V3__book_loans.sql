-- One row per time a student borrows a book. books.student_id stays the current
-- borrower; this table adds the due date and the history.
create table book_loans (
    id                  uuid                        not null,
    book_id             uuid                        not null references books on delete cascade,
    student_id          uuid                        not null references students on delete cascade,
    borrowed_at         timestamp(6) with time zone not null,
    due_at              timestamp(6) with time zone not null,
    returned_at         timestamp(6) with time zone,
    -- set once the overdue check has reported this loan, so it is reported only once
    overdue_notified_at timestamp(6) with time zone,
    primary key (id)
);

create index idx_book_loans_student on book_loans (student_id);
create index idx_book_loans_open on book_loans (book_id) where returned_at is null;

-- Books that were already borrowed before loans existed get an open loan,
-- starting now with the default 14-day period.
insert into book_loans (id, book_id, student_id, borrowed_at, due_at)
select gen_random_uuid(), b.id, b.student_id, now(), now() + interval '14 days'
from books b
where b.student_id is not null;
