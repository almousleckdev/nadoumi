-- Fills the application reference on the payments that already exist. Safe to run again.
--
-- manual rollback: update nad_application_payment set application_reference = null where id > 0;

update nad_application_payment p
  join nad_application a on a.id = p.application_id
   set p.application_reference = a.reference
 where p.id > 0 and p.application_reference is null;
