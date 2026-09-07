-- Editable CNY->USD display rate. Staff enter catalog money in RMB (CNY) only;
-- the USD figure shown beside it is amount_cny * this rate, computed at read time
-- and never stored per record. One number, editable in System > Configuration.
-- RuoYi keeps sys_config warm in Redis (key: sys_config:nadoumi.fx.cny_usd).
--
-- config_type 'Y' = system built-in: editable in the UI, not deletable.
--
-- manual rollback: delete from sys_config where config_key = 'nadoumi.fx.cny_usd';

insert into sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
values ('FX rate CNY to USD', 'nadoumi.fx.cny_usd', '0.1381', 'Y', 'admin', now(),
        'Multiplier applied to a CNY (RMB) amount to display an approximate USD value across the catalog. Staff enter RMB only.');
