-- The Nadoumi service fee a student pays after the university confirms admission, before the admission letter and the
-- JW202 are released. A scholarship already carries one (V16); a programme now does too. Null or zero means no service fee,
-- so nothing is asked and nothing is withheld.
-- Manual rollback: alter table nad_program drop column service_fee_currency, drop column service_fee_amount;

alter table nad_program
  add column service_fee_amount   decimal(12, 2) null comment 'Nadoumi service fee after admission is confirmed' after application_fee_currency,
  add column service_fee_currency char(3)        null comment 'ISO 4217' after service_fee_amount;
