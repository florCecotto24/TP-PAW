-- 1. Add user_role column (single role per user, replaces user_roles table)
ALTER TABLE users
    ADD COLUMN user_role VARCHAR(50) NOT NULL DEFAULT 'USER';

ALTER TABLE users
    ADD CONSTRAINT users_user_role_check CHECK (user_role IN ('USER', 'ADMIN'));

-- 2. Add role_assigned_by (null for USER accounts and for the first manually-promoted admin)
ALTER TABLE users
    ADD COLUMN role_assigned_by INTEGER REFERENCES users(id) ON DELETE SET NULL;

-- 3. Add blocked flag
ALTER TABLE users
    ADD COLUMN blocked BOOLEAN NOT NULL DEFAULT FALSE;

-- 4. Migrate existing ADMIN roles from user_roles
UPDATE users SET user_role = 'ADMIN'
WHERE id IN (SELECT user_id FROM user_roles WHERE role = 'ADMIN');

UPDATE users u SET role_assigned_by = (
    SELECT assigned_by FROM user_roles WHERE user_id = u.id AND role = 'ADMIN'
)
WHERE u.user_role = 'ADMIN';

-- 5. Drop user_roles table
DROP TABLE IF EXISTS user_roles;

-- To promote the first admin (run once manually after deployment):
-- UPDATE users SET user_role = 'ADMIN', role_assigned_by = NULL WHERE email = '<admin_email>';
