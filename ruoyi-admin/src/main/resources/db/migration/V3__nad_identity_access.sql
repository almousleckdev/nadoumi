-- Identity & Access: nad_user_applicant_access (docs/DOMAIN_MODEL.md §4).

create table nad_user_applicant_access (
  id                        bigint       not null auto_increment,
  user_id                   bigint           null,
  applicant_id              bigint       not null,
  application_id            bigint           null,
  access_role               varchar(16)  not null,
  status                    varchar(16)  not null default 'PENDING',
  invited_email             varchar(120)     null,
  capability_overrides_json json             null,
  is_interim                tinyint(1)   not null default 0,
  -- generated guards give INV1/INV2 a plain UNIQUE (MySQL has no partial indexes)
  owner_guard               bigint as (case when access_role = 'OWNER' and status = 'ACTIVE' then applicant_id end) stored,
  active_guard              varchar(96) as (case when status = 'ACTIVE'
                                then concat_ws(':', user_id, applicant_id, coalesce(application_id, 0)) end) stored,
  granted_by_user_id        bigint           null,
  granted_at                datetime         null,
  revoked_by_user_id        bigint           null,
  revoked_at                datetime         null,
  revoke_reason             varchar(255)     null,
  expires_at                datetime         null,
  create_by                 varchar(64)  not null default '',
  create_time               datetime         null,
  update_by                 varchar(64)  not null default '',
  update_time               datetime         null,
  remark                    varchar(500)     null,
  primary key (id),
  unique key uk_uaa_owner_guard  (owner_guard),
  unique key uk_uaa_active_guard (active_guard),
  key idx_uaa_user      (user_id, status),
  key idx_uaa_applicant (applicant_id, status),
  key idx_uaa_sweep     (status, expires_at),
  key idx_uaa_app       (application_id),
  constraint fk_uaa_user    foreign key (user_id)            references sys_user (user_id) on delete restrict,
  constraint fk_uaa_grantor foreign key (granted_by_user_id) references sys_user (user_id) on delete restrict,
  constraint fk_uaa_revoker foreign key (revoked_by_user_id) references sys_user (user_id) on delete restrict
) engine=innodb default charset=utf8mb4;
