-- A short random code printed (inside a QR code) on every generated document. Anyone holding the printed document can
-- check, through a public page that returns only "valid or not", that Nadoumi really issued a document with that number.
-- The code is unguessable (60 bits) and not derived from the document, so it can be printed in the file it protects.
--
-- Written to be safe to run again: MySQL DDL is not transactional, so a run that stops half way leaves the column behind,
-- and a plain "add column" would then fail on every later start. Each DDL step first checks what already exists. The
-- backfill filters on the primary key as well, so it also runs on servers that set sql_safe_updates.
--
-- manual rollback: alter table nad_payment_document drop index idx_payment_document_number, drop column verify_code;

set @has_code := (select count(*) from information_schema.columns
                  where table_schema = database() and table_name = 'nad_payment_document' and column_name = 'verify_code');
set @ddl := if(@has_code = 0,
               'alter table nad_payment_document add column verify_code varchar(16) not null default '''' after doc_number',
               'select 1');
prepare nad_step from @ddl;
execute nad_step;
deallocate prepare nad_step;

update nad_payment_document
   set verify_code = upper(substr(sha2(concat(id, rand(), uuid()), 256), 1, 12))
 where id > 0 and verify_code = '';

set @has_key := (select count(*) from information_schema.statistics
                 where table_schema = database() and table_name = 'nad_payment_document'
                   and index_name = 'idx_payment_document_number');
set @ddl := if(@has_key = 0,
               'alter table nad_payment_document add key idx_payment_document_number (doc_number)',
               'select 1');
prepare nad_step from @ddl;
execute nad_step;
deallocate prepare nad_step;
