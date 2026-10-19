-- A university's office line can list more than one number (e.g. two mobile contacts), which does not fit varchar(32).
--
-- manual rollback (only safe while no value is longer than 32 characters):
--   alter table nad_university modify column office_phone varchar(32) null;

alter table nad_university modify column office_phone varchar(64) null;
