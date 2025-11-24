-- Customer Users Database Schema
-- This schema mirrors the portal_users structure but for customer users

-- Create customer_roles table
CREATE TABLE IF NOT EXISTS customer_roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255)
);

-- Create customer_permissions table
CREATE TABLE IF NOT EXISTS customer_permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(255)
);

-- Create customer_role_permissions junction table
CREATE TABLE IF NOT EXISTS customer_role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES customer_roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES customer_permissions(id) ON DELETE CASCADE
);

-- Create customer_users table
CREATE TABLE IF NOT EXISTS customer_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role_id BIGINT,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES customer_roles(id)
);

-- Create customer_refresh_tokens table
CREATE TABLE IF NOT EXISTS customer_refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT false,
    FOREIGN KEY (user_id) REFERENCES customer_users(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_customer_users_email ON customer_users(email);
CREATE INDEX IF NOT EXISTS idx_customer_users_role ON customer_users(role_id);
CREATE INDEX IF NOT EXISTS idx_customer_refresh_tokens_token ON customer_refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_customer_refresh_tokens_user ON customer_refresh_tokens(user_id);

-- Insert default roles
INSERT INTO customer_roles (name, description) VALUES
    ('CUSTOMER', 'Standard customer role with basic access'),
    ('PREMIUM_CUSTOMER', 'Premium customer with additional features'),
    ('VIP_CUSTOMER', 'VIP customer with full access')
ON CONFLICT (name) DO NOTHING;

-- Insert default permissions
INSERT INTO customer_permissions (name, description) VALUES
    ('VIEW_DASHBOARD', 'Can view customer dashboard'),
    ('VIEW_ORDERS', 'Can view order history'),
    ('CREATE_ORDER', 'Can create new orders'),
    ('EDIT_PROFILE', 'Can edit own profile'),
    ('VIEW_ANALYTICS', 'Can view analytics and reports'),
    ('PREMIUM_FEATURES', 'Access to premium features'),
    ('VIP_SUPPORT', 'Access to VIP support channels')
ON CONFLICT (name) DO NOTHING;

-- Assign permissions to roles
-- CUSTOMER role permissions
INSERT INTO customer_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM customer_roles r
CROSS JOIN customer_permissions p
WHERE r.name = 'CUSTOMER'
  AND p.name IN ('VIEW_DASHBOARD', 'VIEW_ORDERS', 'CREATE_ORDER', 'EDIT_PROFILE')
ON CONFLICT DO NOTHING;

-- PREMIUM_CUSTOMER role permissions
INSERT INTO customer_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM customer_roles r
CROSS JOIN customer_permissions p
WHERE r.name = 'PREMIUM_CUSTOMER'
  AND p.name IN ('VIEW_DASHBOARD', 'VIEW_ORDERS', 'CREATE_ORDER', 'EDIT_PROFILE', 'VIEW_ANALYTICS', 'PREMIUM_FEATURES')
ON CONFLICT DO NOTHING;

-- VIP_CUSTOMER role permissions (all permissions)
INSERT INTO customer_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM customer_roles r
CROSS JOIN customer_permissions p
WHERE r.name = 'VIP_CUSTOMER'
ON CONFLICT DO NOTHING;

-- Insert a test customer user (password: password123)
-- Password hash generated with BCrypt for 'password123'
INSERT INTO customer_users (email, password, first_name, last_name, role_id, enabled)
SELECT 
    'customer@test.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi',
    'Test',
    'Customer',
    (SELECT id FROM customer_roles WHERE name = 'CUSTOMER'),
    true
ON CONFLICT (email) DO NOTHING;

-- Comments for documentation
COMMENT ON TABLE customer_users IS 'Customer user accounts for the WallDot customer portal';
COMMENT ON TABLE customer_roles IS 'Customer role definitions with hierarchical permissions';
COMMENT ON TABLE customer_permissions IS 'Granular permissions for customer features';
COMMENT ON TABLE customer_refresh_tokens IS 'JWT refresh tokens for customer authentication';

