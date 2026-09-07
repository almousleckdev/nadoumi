# Database rules

Operationalizes `CLAUDE.md` §11, §14. Keep `docs/DATABASE_DESIGN.md` in sync.

## Engine and schema ownership

- MySQL. No PostgreSQL or second datastore without an approved `docs/` decision
  (the ECC `postgres-patterns` skill is reference only — not a licence to switch).
- Preserve the RuoYi baseline schema where it is appropriate. RuoYi tables keep
  the `sys_` prefix; Nadoumi tables use the `nad_` prefix.
- Do not create a table just because a CRUD screen wants one. Model the entity
  first.

## Migrations (Flyway)

- Location: `ruoyi-admin/src/main/resources/db/migration/`.
- Naming: `V<n>__nad_<area>.sql`, `<n>` = next unused sequential integer
  (baseline `V1`, latest currently `V14`). One migration per logical change.
- Migrations are **append-only and immutable** once merged. Never edit a shipped
  `V*` file — add a new one. Fixes to a released migration go in a new `V*`.
- Every migration is reversible in principle: describe the manual rollback in a
  header comment when an automated down-migration is not provided.
- Seed/reference data goes in its own clearly-named migration
  (`V*__nad_*_seed.sql`), separate from DDL.

## Table design checklist (per important entity)

- [ ] Surrogate `bigint` primary key.
- [ ] Foreign keys declared with explicit `ON DELETE` behaviour (prefer
      `RESTRICT`; `CASCADE` only for true ownership).
- [ ] Unique constraints for real business keys.
- [ ] Indexes for every FK and every column used in a `WHERE` / `ORDER BY` on a
      hot path.
- [ ] Audit columns: `create_by, create_time, update_by, update_time`
      (RuoYi `BaseEntity` convention); add `del_flag` only if soft delete is
      genuinely required.
- [ ] Lifecycle / status columns use a documented enum of string codes, not
      magic integers.
- [ ] Confidential relationships (e.g. scholarship → partner university) are
      queryable only through staff-authorized code paths — the FK existing is not
      permission to expose it (`security.md`).
- [ ] Money as `decimal`, never `float`/`double`. Timestamps in UTC.

## Access

- All SQL through MyBatis mappers with parameter binding. No dynamic SQL string
  concatenation. Keep mapper XML close to the module that owns the table.

## Reference

ECC skills: `database-migrations`, `mysql-patterns`, `jpa-patterns` (concepts,
adapt to MyBatis). Agent: `nadoumi` `database` review via `nadoumi-code-reviewer`
or ECC `database-reviewer`.
