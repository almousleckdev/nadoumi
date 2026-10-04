-- Content domain: news articles, their uploaded images and threaded comments.
-- Body is Markdown text; image binaries live in object storage (nad_media_asset), referenced by media id.
--
-- manual rollback:
--   drop table nad_article_comment; drop table nad_article_image; drop table nad_article;

create table nad_article (
  id             bigint        not null auto_increment,
  slug           varchar(160)  not null comment 'public URL identifier',
  title          varchar(200)  not null,
  subtitle       varchar(300)  default null,
  body_md        mediumtext    not null comment 'Markdown; inline images are absolute media URLs',
  cover_media_id bigint        default null comment 'nad_media_asset id of the cover image',
  language       varchar(8)    not null default 'en',
  status         varchar(16)   not null default 'DRAFT' comment 'DRAFT, PUBLISHED, UNPUBLISHED',
  published_at   datetime      default null comment 'first publish time (UTC)',
  author_id      bigint        not null comment 'sys_user of the staff author',
  create_by      varchar(64)   default '',
  create_time    datetime      default null,
  update_by      varchar(64)   default '',
  update_time    datetime      default null,
  primary key (id),
  unique key uk_nad_article_slug (slug),
  key idx_nad_article_status_pub (status, published_at),
  key idx_nad_article_author (author_id)
) engine=InnoDB default charset=utf8mb4 comment='News article';

create table nad_article_image (
  id          bigint      not null auto_increment,
  article_id  bigint      not null,
  media_id    bigint      not null comment 'nad_media_asset id',
  sort_order  int         not null default 0,
  create_by   varchar(64) default '',
  create_time datetime    default null,
  primary key (id),
  key idx_nad_article_image_article (article_id),
  constraint fk_nad_article_image_article foreign key (article_id) references nad_article (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 comment='Images uploaded for an article body';

create table nad_article_comment (
  id           bigint       not null auto_increment,
  article_id   bigint       not null,
  parent_id    bigint       default null comment 'null = top-level comment; otherwise the comment replied to',
  author_id    bigint       not null comment 'sys_user of the commenter',
  body         varchar(2000) not null,
  status       varchar(16)  not null default 'VISIBLE' comment 'VISIBLE, DELETED',
  deleted_by   bigint       default null comment 'sys_user of the moderator',
  deleted_time datetime     default null,
  create_time  datetime     not null,
  primary key (id),
  key idx_nad_comment_article (article_id, create_time),
  key idx_nad_comment_parent (parent_id),
  key idx_nad_comment_author (author_id),
  constraint fk_nad_comment_article foreign key (article_id) references nad_article (id) on delete cascade,
  constraint fk_nad_comment_parent foreign key (parent_id) references nad_article_comment (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 comment='Threaded article comments; staff deletion is soft so replies keep their place. A thread is owned by its article, hence the cascades.';
