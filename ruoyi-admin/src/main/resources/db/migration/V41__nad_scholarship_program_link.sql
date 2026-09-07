-- Operational link from a scholarship to the specific programme it funds.
--
-- CONFIDENTIALITY: a programme belongs to a university, so this association would
-- reveal the partner institution. It therefore lives in the CONFIDENTIAL
-- nad_scholarship_internal table (never joined by a public / student query),
-- next to university_id, and is only readable/writable by
-- nad:scholarship:internal:* holders.
--
-- manual rollback:
--   alter table nad_scholarship_internal drop foreign key fk_sch_internal_program;
--   alter table nad_scholarship_internal drop column program_id;

alter table nad_scholarship_internal
  add column program_id bigint null comment 'FK nad_program -- the programme this award funds' after university_id,
  add key idx_scholarship_internal_program (program_id),
  add constraint fk_sch_internal_program foreign key (program_id)
    references nad_program (id) on delete set null;
