-- News: an opaque public identifier for articles (staff URLs must not expose a guessable 1, 2, 3 sequence)
-- and reader likes on articles and comments.
--
-- public_id is added nullable, backfilled one UUID per existing row, then made NOT NULL with a
-- server-side default so inserts need no application-generated value. nad_article.id stays the
-- surrogate primary key and the target of every foreign key.
--
-- Likes are one row per (item, user): the composite primary key makes a repeated like a no-op and
-- gives the per-item count an index. Deleting an article or comment cascades its likes.
--
-- manual rollback:
--   drop table nad_article_comment_like; drop table nad_article_like;
--   alter table nad_article drop index uk_nad_article_public_id, drop column public_id;

alter table nad_article
  add column public_id char(36) default null comment 'opaque UUID used in staff URLs' after id;

update nad_article set public_id = uuid() where public_id is null;

alter table nad_article
  modify column public_id char(36) not null default (uuid()) comment 'opaque UUID used in staff URLs',
  add unique key uk_nad_article_public_id (public_id);

create table nad_article_like (
  article_id  bigint   not null,
  user_id     bigint   not null comment 'sys_user of the reader',
  create_time datetime not null,
  primary key (article_id, user_id),
  key idx_nad_article_like_user (user_id),
  constraint fk_nad_article_like_article foreign key (article_id) references nad_article (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 comment='Reader likes on an article, one per user';

create table nad_article_comment_like (
  comment_id  bigint   not null,
  user_id     bigint   not null comment 'sys_user of the reader',
  create_time datetime not null,
  primary key (comment_id, user_id),
  key idx_nad_comment_like_user (user_id),
  constraint fk_nad_comment_like_comment foreign key (comment_id) references nad_article_comment (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 comment='Reader likes on a comment, one per user';
