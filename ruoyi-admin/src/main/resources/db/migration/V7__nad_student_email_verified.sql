-- Student identity is email-first (spec Revision 2, D-R2-2 / §15.3).
-- The verified email is the login key for user_type='10'; user_name becomes a
-- server-generated internal handle.

ALTER TABLE sys_user
    ADD COLUMN email_verified tinyint(1) NOT NULL DEFAULT 0
    COMMENT '1 = email address confirmed via an OTP flow';

-- Fast lookup by email for the student login path. NOT unique at the DB level
-- (staff rows may legitimately share or blank their email); uniqueness among
-- user_type='10' is enforced in StudentAuthService.register.
CREATE INDEX idx_sys_user_email ON sys_user (email);
