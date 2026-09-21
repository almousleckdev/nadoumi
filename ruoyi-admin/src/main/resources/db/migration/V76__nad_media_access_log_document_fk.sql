-- nad_media_access_log.document_id already exists as a nullable column (V26,
-- "set once the document slice exists") but has never had an FK, since
-- nad_document did not exist yet. It exists now (V71) — add the FK.
--
-- manual rollback: alter table nad_media_access_log drop foreign key fk_mal_document;

alter table nad_media_access_log
  add constraint fk_mal_document foreign key (document_id)
    references nad_document (id) on delete set null;
