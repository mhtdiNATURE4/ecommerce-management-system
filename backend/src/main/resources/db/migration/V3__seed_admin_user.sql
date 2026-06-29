-- V3: seed admin user (idempotent)
INSERT INTO users (name, email, password, role, created_at, updated_at)
SELECT 'admin', 'admin@example.com', '$2a$10$rYngRYkbVPVcrk44yfHlyeE7dNjKdVP8Yrr2nZBSBp.iQkQJE.Tz.', 'ADMIN', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email='admin@example.com');