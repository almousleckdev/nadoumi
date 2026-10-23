-- Which types of scholarship fund a programme. Some programmes have scholarships and some have none, so the link table is
-- simply empty for a programme without any. The category codes come from nad_scholarship_category (CSC, provincial,
-- university, ...); they are stored as codes, validated by the service through the shared catalogue interface, so the
-- programme tables do not depend on the scholarship tables. The free-text scholarship_info stays for the details.
--
-- manual rollback: drop table nad_program_scholarship_type;

create table nad_program_scholarship_type (
  program_id    bigint      not null,
  category_code varchar(32) not null,
  primary key (program_id, category_code),
  key idx_program_scholarship_type_code (category_code),
  constraint fk_program_scholarship_type_program foreign key (program_id)
    references nad_program (id) on delete cascade
) engine=innodb default charset=utf8mb4;
