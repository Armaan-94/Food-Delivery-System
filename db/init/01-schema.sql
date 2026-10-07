-- Schema for the food delivery platform. Each service owns its own database.
-- Works on MySQL 8+. Run once, as a user that can create databases:
--   mysql -u root -p < db/init/01-schema.sql
-- docker-compose runs everything in this folder automatically on first start.

CREATE DATABASE IF NOT EXISTS user_db     CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS delivery_db CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS order_db    CHARACTER SET utf8mb4;

-- ---------------------------------------------------------------------------
-- user-service: the single store for accounts, credentials and roles.
-- password_hash holds a BCrypt hash; plaintext passwords are never stored.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_db.users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- delivery-service
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS delivery_db.partners (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20)  NULL,
    vehicle_type VARCHAR(50)  NOT NULL,
    available    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS delivery_db.deliveries (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    order_id         BIGINT       NOT NULL,
    partner_id       BIGINT       NULL,
    status           VARCHAR(20)  NOT NULL,
    pickup_location  VARCHAR(255) NOT NULL,
    dropoff_location VARCHAR(255) NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_deliveries_order (order_id),
    KEY ix_deliveries_partner (partner_id),
    CONSTRAINT fk_deliveries_partner FOREIGN KEY (partner_id) REFERENCES partners (id) ON DELETE RESTRICT,
    CONSTRAINT chk_deliveries_status CHECK (status IN
        ('PENDING', 'ASSIGNED', 'PICKED_UP', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- order-service (Hibernate only validates this table, it never creates it)
-- user_id is the id of the account that placed the order (the JWT subject).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_db.food_orders (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    user_id         BIGINT         NOT NULL,
    customer_name   VARCHAR(255)   NOT NULL,
    order_details   VARCHAR(1000)  NULL,
    total_amount    DECIMAL(10, 2) NOT NULL,
    order_status    VARCHAR(30)    NOT NULL,
    order_timestamp DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY ix_food_orders_user (user_id),
    KEY ix_food_orders_timestamp (order_timestamp)
) ENGINE = InnoDB;
