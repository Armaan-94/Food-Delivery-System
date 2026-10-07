CREATE TABLE IF NOT EXISTS food_orders (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    user_id         BIGINT         NOT NULL,
    customer_name   VARCHAR(255)   NOT NULL,
    order_details   VARCHAR(1000)  NULL,
    total_amount    DECIMAL(10, 2) NOT NULL,
    order_status    VARCHAR(30)    NOT NULL,
    order_timestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
