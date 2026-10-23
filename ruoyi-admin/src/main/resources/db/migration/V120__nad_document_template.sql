-- Files a programme or scholarship gives the applicant to fill in and send back: for example the application form
-- template. An admin uploads them; the applicant downloads the ones that belong to the opportunity they apply for. The
-- bytes are a PROTECTED media asset (DOCUMENT_TEMPLATE); this table is the business record (polymorphic scope + ref_id,
-- like nad_document_requirement, checked by the service rather than a foreign key).
--
-- manual rollback: drop table nad_document_template;

create table nad_document_template (
  id           bigint       not null auto_increment,
  scope        varchar(16)  not null comment 'PROGRAM|SCHOLARSHIP',
  ref_id       bigint       not null comment 'the programme or scholarship id',
  title        varchar(120) not null,
  file_name    varchar(120) not null,
  media_id     bigint       not null,
  create_by    varchar(64)  not null default '',
  create_time  datetime         null,
  primary key (id),
  key idx_document_template_scope (scope, ref_id),
  constraint fk_document_template_media foreign key (media_id) references nad_media_asset (id) on delete restrict
) engine=InnoDB default charset=utf8mb4;
