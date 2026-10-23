-- The application reference (NAD20261023001) is kept on each payment, like the applicant's name already is, so the
-- verification queue, receipts and emails show it without reaching into the application tables.
--
-- manual rollback: alter table nad_application_payment drop column application_reference;

alter table nad_application_payment
  add column application_reference varchar(24) null after application_id;
