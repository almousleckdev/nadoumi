-- A scholarship is standalone (not linked to a university), so an application can be for a scholarship alone.
-- Such an application has no programme: program_id becomes nullable. Which combinations are valid per
-- application_type is enforced in ApplicationShape (service layer), as before for scholarship_id.
--
-- manual rollback (only valid while no SCHOLARSHIP_ONLY application exists):
--   alter table nad_application modify program_id bigint not null;

alter table nad_application modify program_id bigint null;
