CREATE TABLE IF NOT EXISTS partners (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20)  NULL,
    vehicle_type VARCHAR(50)  NOT NULL,
    available    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS deliveries (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    order_id         BIGINT       NOT NULL,
    partner_id       BIGINT       NULL,
    status           VARCHAR(20)  NOT NULL,
    pickup_location  VARCHAR(255) NOT NULL,
    dropoff_location VARCHAR(255) NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_deliveries_order UNIQUE (order_id),
    CONSTRAINT fk_deliveries_partner FOREIGN KEY (partner_id) REFERENCES partners (id) ON DELETE RESTRICT
);
