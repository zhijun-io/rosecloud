-- Index the token columns so the per-request revocation lookup
-- (LoginSessionServiceImpl#isRevoked) stays index-backed. The columns are
-- TEXT, so prefix indexes are used; JWTs are long and vary early, so a
-- 255-char prefix is highly selective.
ALTER TABLE auth_login_session
    ADD KEY idx_token (token(255)),
    ADD KEY idx_refresh_token (refresh_token(255));
