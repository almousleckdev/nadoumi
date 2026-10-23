-- "Recommended by Nadoumi" for programmes, like scholarships and universities already have. Staff switch it on; the public
-- catalogue can filter on it and the home page lists those programmes.
--
-- manual rollback: alter table nad_program drop key idx_program_recommended, drop column is_recommended;

alter table nad_program
  add column is_recommended tinyint(1) not null default 0 after is_hot,
  add key idx_program_recommended (is_recommended, is_featured);
