-- Revision 2 §17: remove the confusing RuoYi "your password is still the initial
-- password" prompt. Forced first-login rotation for seeded accounts (almousleck,
-- pwd_update_date IS NULL) is carried by the password-expiry path instead.

UPDATE sys_config SET config_value = '0'  WHERE config_key = 'sys.account.initPasswordModify';
UPDATE sys_config SET config_value = '90' WHERE config_key = 'sys.account.passwordValidateDays';
