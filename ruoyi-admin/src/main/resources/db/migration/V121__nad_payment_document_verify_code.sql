-- A short random code printed (inside a QR code) on every generated document. Anyone holding the printed document can
-- check, through a public page that returns only "valid or not", that Nadoumi really issued a document with that number.
-- The code is unguessable (60 bits) and not derived from the document, so it can be printed in the file it protects.
--
-- manual rollback: alter table nad_payment_document drop index idx_payment_document_number, drop column verify_code;

alter table nad_payment_document add column verify_code varchar(16) not null default '' after doc_number;
update nad_payment_document set verify_code = upper(substr(md5(concat(id, rand(), uuid())), 1, 12)) where verify_code = '';
alter table nad_payment_document add key idx_payment_document_number (doc_number);
