-- A student's in-app inbox (the "inbox" notification channel).
create table notifications (
    id         uuid                        not null,
    student_id uuid                        not null references students on delete cascade,
    subject    varchar(255)                not null,
    body       text                        not null,
    created_at timestamp(6) with time zone not null,
    -- null until the student has read it
    read_at    timestamp(6) with time zone,
    primary key (id)
);

create index idx_notifications_student on notifications (student_id, created_at desc);
