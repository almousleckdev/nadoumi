-- Every uploaded document version was stored with scan_status 'PENDING': the service stamps it CLEAN (no malware scanner is
-- wired yet) but the insert statement wrote a hard-coded 'PENDING', and a download is refused unless the version is CLEAN. So
-- no uploaded document could ever be viewed or downloaded, by the student or by staff. The insert now writes the real value;
-- this marks the versions that were already uploaded the same way the service would have. Safe to run again.
--
-- manual rollback: not reversible, and there is nothing to roll back to: PENDING blocked every download.

update nad_document_version set scan_status = 'CLEAN' where id > 0 and scan_status = 'PENDING';
