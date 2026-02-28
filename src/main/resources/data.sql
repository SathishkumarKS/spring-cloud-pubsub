INSERT INTO orders (id, customer_name, product, quantity, price, status, created_at, updated_at)
VALUES
  ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Alice Johnson', 'Laptop Pro', 1, 1299.9900, 'CONFIRMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'Bob Smith', 'Wireless Headphones', 2, 79.9900, 'CREATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
