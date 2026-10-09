-- A staff photo is stored as its Cloudinary delivery URL, which is longer than RuoYi's stock varchar(100)
-- (the upload failed with "Data too long for column 'avatar'", so staff photos never saved).
--
-- manual rollback (only safe while no stored URL is longer than 100 characters):
--   alter table sys_user modify column avatar varchar(100) null default '';
alter table sys_user modify column avatar varchar(512) null default '' comment 'profile photo URL';
