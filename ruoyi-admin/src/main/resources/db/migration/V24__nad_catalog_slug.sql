-- Public slug identifiers for universities and programmes, so public URLs read
-- `/universities/fudan-university` and `/programs/fudan-mba` instead of exposing
-- sequential database ids (`/universities/1`). Scholarships already have a slug.
--
-- Internal primary keys stay `bigint` -- this only adds an opaque public handle.
--
-- Rollback:
--   alter table nad_university drop column slug;
--   alter table nad_program    drop column slug;

alter table nad_university add column slug varchar(160) null after name_cn;
alter table nad_program    add column slug varchar(160) null after name_cn;

-- backfill from the name, kebab-cased
update nad_university
  set slug = nullif(lower(regexp_replace(regexp_replace(name, '[^0-9A-Za-z]+', '-'), '(^-)|(-$)', '')), '')
  where slug is null;
update nad_program
  set slug = nullif(lower(regexp_replace(regexp_replace(name, '[^0-9A-Za-z]+', '-'), '(^-)|(-$)', '')), '')
  where slug is null;

-- fallback for any row whose name produced nothing usable
update nad_university set slug = concat('university-', id) where slug is null or slug = '';
update nad_program    set slug = concat('programme-', id)  where slug is null or slug = '';

-- de-duplicate collisions by appending the row id (keep the lowest id unchanged)
update nad_university u
  join (select slug, min(id) as keep_id from nad_university group by slug having count(*) > 1) d
    on u.slug = d.slug and u.id <> d.keep_id
  set u.slug = concat(u.slug, '-', u.id);
update nad_program p
  join (select slug, min(id) as keep_id from nad_program group by slug having count(*) > 1) d
    on p.slug = d.slug and p.id <> d.keep_id
  set p.slug = concat(p.slug, '-', p.id);

alter table nad_university modify column slug varchar(160) not null;
alter table nad_program    modify column slug varchar(160) not null;
alter table nad_university add unique key uk_university_slug (slug);
alter table nad_program    add unique key uk_program_slug (slug);
