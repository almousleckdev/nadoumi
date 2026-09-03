-- University gallery: up to a handful of images (campus, dormitory, campus life…)
-- with captions, shown on the public university page. URL-based for now — a real
-- upload path arrives with the Document slice. Covered by nad:university:* perms.

create table nad_university_gallery (
  id            bigint       not null auto_increment,
  university_id bigint       not null,
  image_url     varchar(500) not null,
  caption       varchar(200)     null,
  sort_order    int          not null default 0,
  primary key (id),
  key idx_university_gallery (university_id, sort_order),
  constraint fk_uni_gallery_university foreign key (university_id)
    references nad_university (id) on delete cascade
) engine=innodb default charset=utf8mb4;
