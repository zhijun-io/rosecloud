-- Passwords now live in the auth service's auth_credential table; the system service no longer
-- stores or reads them. Keep the activation-token columns on user_credential (activation is
-- still a system concern). See docs/sdd/2026-07-13-auth-iam-core-spec.md (Slice B).
ALTER TABLE user_credential
    DROP COLUMN IF EXISTS password,
    DROP COLUMN IF EXISTS password_changed_time,
    DROP COLUMN IF EXISTS last_login_time;
