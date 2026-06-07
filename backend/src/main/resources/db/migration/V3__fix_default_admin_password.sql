-- Fix the seeded admin password hash so it matches the documented password.
-- Default admin credentials:
--   email: admin@parksmart.com
--   password: Admin@123
UPDATE users
SET password = '$2a$12$eSHt1PgGEvVQca6VWmSFbeRKyPqfSsop.t2Ib/tXynf4Mfq/2O0LC'
WHERE email = 'admin@parksmart.com';
