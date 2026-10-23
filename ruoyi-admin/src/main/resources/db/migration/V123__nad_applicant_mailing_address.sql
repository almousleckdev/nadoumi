-- The application form asks for a current address with a post code and says where the admission documents are sent.
-- The current address itself lives in nad_applicant_residence; its post code and the delivery address are kept with the
-- other personal details. mailing_choice says which address receives the admission documents: the current one, the
-- permanent one, or a separate one typed in mailing_address (mailing_address is only read for OTHER).
--
-- manual rollback: alter table nad_applicant_personal drop column current_postcode, drop column mailing_choice,
--   drop column mailing_address, drop column mailing_postcode;

alter table nad_applicant_personal
  add column current_postcode  varchar(20)  null after permanent_postcode,
  add column mailing_choice    varchar(16)  not null default 'CURRENT' after current_postcode,
  add column mailing_address   varchar(300) null after mailing_choice,
  add column mailing_postcode  varchar(20)  null after mailing_address;
