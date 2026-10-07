-- Optional demo data. There is no endpoint for placing orders yet, so orders have to be inserted directly.
-- Replace the user_id values with real ids from user_db.users (sign up first, then look up the id).

INSERT INTO order_db.food_orders (user_id, customer_name, order_details, total_amount, order_status)
VALUES
    (1, 'Demo Customer', '2 x Margherita pizza, 1 x Garlic bread', 24.50, 'PLACED'),
    (1, 'Demo Customer', '1 x Veg burger, 1 x Fries',              11.90, 'DELIVERED');
