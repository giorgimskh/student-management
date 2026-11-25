-- Databases upgraded with ddl-auto=update got the status column as nullable, so
-- students created before it existed have no status. They count as ACTIVE.
-- add column if not exists: covers a database that was baselined before the
-- status column was added at all.
alter table students add column if not exists status varchar(20);
update students set status = 'ACTIVE' where status is null;
alter table students alter column status set not null;
