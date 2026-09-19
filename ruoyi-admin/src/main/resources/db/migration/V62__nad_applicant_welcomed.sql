-- Onboarding v2, slice 4: the welcome celebration is shown once. welcomed_at is set when the student has seen it
-- (only after onboarded_at); NULL with onboarded_at set means the celebration is still pending.
-- Rollback (manual): alter table nad_applicant drop column welcomed_at;

alter table nad_applicant
  add column welcomed_at datetime null after onboarded_at;
