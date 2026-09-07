-- Structure the employee emergency contact: the existing free-text column keeps
-- the contact's NAME; add relationship, phone and (optional) email.
--
-- manual rollback:
--   alter table nad_employee
--     drop column emergency_contact_relationship,
--     drop column emergency_contact_phone,
--     drop column emergency_contact_email;

alter table nad_employee
  add column emergency_contact_relationship varchar(60)  null after emergency_contact,
  add column emergency_contact_phone        varchar(32)  null after emergency_contact_relationship,
  add column emergency_contact_email        varchar(120) null after emergency_contact_phone;
