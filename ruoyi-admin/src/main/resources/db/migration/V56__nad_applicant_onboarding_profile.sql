-- Applicant onboarding v2, slice 1: profile fields and the server-side completion record.
--   gender / country_of_origin / country_of_residence / native_language / wechat_id / whatsapp: profile answers
--   email_verified_at: when the contact email was proven by a one-time code (NULL = unverified)
--   onboarded_at: set only by the onboarding-complete endpoint; NULL = onboarding not finished
-- Existing rows keep onboarded_at NULL on purpose: they must finish the new onboarding.
-- Rollback (manual): alter table nad_applicant drop column onboarded_at, drop column email_verified_at,
--   drop column whatsapp, drop column wechat_id, drop column native_language,
--   drop column country_of_residence, drop column country_of_origin, drop column gender;

alter table nad_applicant
  add column gender               varchar(16) null after nationality,
  add column country_of_origin    varchar(2)  null after gender,
  add column country_of_residence varchar(2)  null after country_of_origin,
  add column native_language      varchar(8)  null after country_of_residence,
  add column wechat_id            varchar(64) null after phone,
  add column whatsapp             varchar(32) null after wechat_id,
  add column email_verified_at    datetime    null after whatsapp,
  add column onboarded_at         datetime    null after email_verified_at,
  add key idx_applicant_onboarded (onboarded_at);
