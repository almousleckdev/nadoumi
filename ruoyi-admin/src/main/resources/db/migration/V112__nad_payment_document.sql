-- Documents generated when an application fee is confirmed: the payment receipt and the Nadoumi application form.
-- The PDF bytes live in object storage (media asset, SENSITIVE); this table is the business record.
--
--   RECEIPT           one per confirmed payment, emailed to the student
--   APPLICATION_FORM  version 1 is made at confirmation (the student can download it); version 2 is made when the
--                     student submits and is the copy emailed to the Nadoumi administrators
--
-- delivered_at is set once every intended recipient has been sent the document.
--
-- Integrity: uk_payment_document stops a second render of the same version; uk_payment_document_delivery is the
-- "claim" taken before an email is sent, so a retry can never send the same document to the same person twice.
--
-- manual rollback:
--   drop table if exists nad_payment_document_delivery;
--   drop table if exists nad_payment_document;

create table nad_payment_document (
    id            bigint       not null auto_increment,
    payment_id    bigint       not null,
    application_id bigint      not null,
    kind          varchar(20)  not null,
    version       int          not null default 1,
    doc_number    varchar(40)  not null,
    media_id      bigint       null,
    sha256        char(64)     not null,
    byte_size     int          not null,
    generated_at  datetime     not null,
    delivered_at  datetime     null,
    create_by     varchar(64)  not null default '',
    create_time   datetime     null,
    primary key (id),
    unique key uk_payment_document (payment_id, kind, version),
    key idx_payment_document_application (application_id),
    constraint fk_payment_document_payment foreign key (payment_id) references nad_application_payment (id) on delete restrict,
    constraint fk_payment_document_media foreign key (media_id) references nad_media_asset (id) on delete set null
) engine = InnoDB default charset = utf8mb4;

create table nad_payment_document_delivery (
    id                bigint   not null auto_increment,
    document_id       bigint   not null,
    recipient_user_id bigint   not null,
    claimed_at        datetime not null,
    sent_at           datetime null,
    primary key (id),
    unique key uk_payment_document_delivery (document_id, recipient_user_id),
    constraint fk_payment_document_delivery_doc foreign key (document_id) references nad_payment_document (id) on delete cascade
) engine = InnoDB default charset = utf8mb4;
