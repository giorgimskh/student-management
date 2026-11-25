-- The schema as Hibernate's ddl-auto created it before Flyway was introduced.
-- Existing databases already have these tables: they are baselined at version 1
-- (spring.flyway.baseline-on-migrate) and this script is skipped for them.

create table students (
    id         uuid not null,
    created_at timestamp(6) with time zone,
    email      varchar(255),
    name       varchar(255),
    status     varchar(20) check (status in ('ACTIVE', 'SUSPENDED', 'GRADUATED', 'WITHDRAWN')),
    primary key (id)
);

create table books (
    id         uuid not null,
    isbn       varchar(255),
    title      varchar(255),
    student_id uuid,
    primary key (id)
);

create table courses (
    id          uuid not null,
    code        varchar(255),
    course_name varchar(255),
    primary key (id)
);

create table student_courses (
    student_id uuid not null,
    course_id  uuid not null,
    primary key (student_id, course_id)
);

create table student_photos (
    id                uuid         not null,
    content_type      varchar(255) not null,
    data              bytea        not null,
    original_filename varchar(255),
    size_bytes        bigint       not null,
    uploaded_at       timestamp(6) with time zone,
    student_id        uuid         not null,
    primary key (id)
);

alter table books add constraint fk_books_student foreign key (student_id) references students;
alter table student_courses add constraint fk_student_courses_course foreign key (course_id) references courses;
alter table student_courses add constraint fk_student_courses_student foreign key (student_id) references students;
alter table student_photos add constraint fk_student_photos_student foreign key (student_id) references students;
alter table student_photos add constraint uk_student_photos_student unique (student_id);
