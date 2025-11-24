-- Sample Customer Data for Testing
-- Password for all test users: password123
-- BCrypt hash: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi

-- Sample CUSTOMER users
INSERT INTO customer_users (email, password, first_name, last_name, role_id, enabled)
SELECT 
    'john.doe@example.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi',
    'John',
    'Doe',
    (SELECT id FROM customer_roles WHERE name = 'CUSTOMER'),
    true
ON CONFLICT (email) DO NOTHING;

INSERT INTO customer_users (email, password, first_name, last_name, role_id, enabled)
SELECT 
    'jane.smith@example.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi',
    'Jane',
    'Smith',
    (SELECT id FROM customer_roles WHERE name = 'CUSTOMER'),
    true
ON CONFLICT (email) DO NOTHING;

-- Sample PREMIUM_CUSTOMER users
INSERT INTO customer_users (email, password, first_name, last_name, role_id, enabled)
SELECT 
    'premium@example.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi',
    'Premium',
    'User',
    (SELECT id FROM customer_roles WHERE name = 'PREMIUM_CUSTOMER'),
    true
ON CONFLICT (email) DO NOTHING;

INSERT INTO customer_users (email, password, first_name, last_name, role_id, enabled)
SELECT 
    'michael.premium@example.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi',
    'Michael',
    'Premium',
    (SELECT id FROM customer_roles WHERE name = 'PREMIUM_CUSTOMER'),
    true
ON CONFLICT (email) DO NOTHING;

-- Sample VIP_CUSTOMER users
INSERT INTO customer_users (email, password, first_name, last_name, role_id, enabled)
SELECT 
    'vip@example.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi',
    'VIP',
    'Customer',
    (SELECT id FROM customer_roles WHERE name = 'VIP_CUSTOMER'),
    true
ON CONFLICT (email) DO NOTHING;

-- Add the specific test user for login testing
INSERT INTO customer_users (email, password, first_name, last_name, role_id, enabled)
SELECT 
    'n@gmail.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi',
    'Test',
    'User',
    (SELECT id FROM customer_roles WHERE name = 'CUSTOMER'),
    true
ON CONFLICT (email) DO NOTHING;

-- Display created users
SELECT 
    u.id,
    u.email,
    u.first_name,
    u.last_name,
    r.name as role,
    u.enabled,
    u.created_at
FROM customer_users u
LEFT JOIN customer_roles r ON u.role_id = r.id
ORDER BY u.id;

