-- Gives the application user access to the three service databases.
-- The user must already exist. docker-compose creates it from MYSQL_USER / MYSQL_PASSWORD; for a manual
-- install create it first, for example:
--   CREATE USER 'food_delivery'@'%' IDENTIFIED BY '<choose a password>';
-- Hardening option: create one user per database instead and give each service its own credentials.

GRANT SELECT, INSERT, UPDATE, DELETE ON user_db.*     TO 'food_delivery'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON delivery_db.* TO 'food_delivery'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON order_db.*    TO 'food_delivery'@'%';
