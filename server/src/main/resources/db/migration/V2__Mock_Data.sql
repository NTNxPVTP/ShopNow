-- Chèn 3 người dùng: 1 Admin, 1 Seller, 1 Customer
INSERT INTO users (email, name, password, role) VALUES
('admin@test.com', 'System Admin', '$2a$10$.4p5.Dj7XCM/s/PaH3nVtejZRNN7VgdnKlo378KnzK.sNh9lkuhLC', 'ADMIN'),
('seller1@test.com', 'Nguyen Van Ban', '$2a$10$.4p5.Dj7XCM/s/PaH3nVtejZRNN7VgdnKlo378KnzK.sNh9lkuhLC', 'SELLER'),
('customer1@test.com', 'Tran Thi Mua', '$2a$10$.4p5.Dj7XCM/s/PaH3nVtejZRNN7VgdnKlo378KnzK.sNh9lkuhLC', 'CUSTOMER');

-- Tạo cửa hàng cho Seller
INSERT INTO shops (owner_id, name, address)
SELECT id, 'Tiệm Đồ Điện Tử ABC', '123 Đường Lê Lợi, TP.HCM' 
FROM users WHERE email = 'seller1@test.com';

-- Tạo danh mục sản phẩm
INSERT INTO categories (name) VALUES 
('Điện thoại'), ('Phụ kiện'), ('Máy tính bảng');

-- Chèn sản phẩm vào shop
INSERT INTO products (shop_id, name, quantity, price, status)
SELECT id, 'iPhone 15 Pro Max', 10, 30000000.00, 'ACTIVE' FROM shops WHERE name = 'Tiệm Đồ Điện Tử ABC';

INSERT INTO products (shop_id, name, quantity, price, status)
SELECT id, 'Sạc dự phòng 20000mAh', 50, 500000.00, 'ACTIVE' FROM shops WHERE name = 'Tiệm Đồ Điện Tử ABC';

-- Liên kết sản phẩm với danh mục
INSERT INTO product_categories (product_id, category_id)
SELECT p.id, c.id FROM products p, categories c 
WHERE p.name = 'iPhone 15 Pro Max' AND c.name = 'Điện thoại';

-- Tạo giỏ hàng ban đầu cho khách hàng
INSERT INTO carts (customer_id, total_product, total_price)
SELECT id, 1, 500000.00 FROM users WHERE email = 'customer1@test.com';

-- Thêm sản phẩm vào giỏ hàng
INSERT INTO cart_product (cart_id, product_id, quantity)
SELECT c.id, p.id, 1 
FROM carts c, products p, users u 
WHERE c.customer_id = u.id AND u.email = 'customer1@test.com' AND p.name = 'Sạc dự phòng 20000mAh';

-- Đánh giá sản phẩm
INSERT INTO reviews (reviewer_id, product_id, content, rating)
SELECT u.id, p.id, 'Sản phẩm rất tốt, giao hàng nhanh!', 5
FROM users u, products p 
WHERE u.email = 'customer1@test.com' AND p.name = 'Sạc dự phòng 20000mAh';

INSERT INTO categories (name) VALUES 
('Đồ công nghệ'), 
('Hàng cao cấp'), 
('Giảm giá shock'), 
('Gaming'), 
('Thiết bị làm việc');

-- Lấy ID của shop đã tạo ở bước trước (Tiệm Đồ Điện Tử ABC)
INSERT INTO products (shop_id, name, quantity, price, status)
SELECT id, 'Bàn phím cơ AKKO', 20, 1500000.00, 'ACTIVE' FROM shops WHERE name = 'Tiệm Đồ Điện Tử ABC';

INSERT INTO products (shop_id, name, quantity, price, status)
SELECT id, 'Chuột Logitech G502', 15, 1200000.00, 'ACTIVE' FROM shops WHERE name = 'Tiệm Đồ Điện Tử ABC';

INSERT INTO products (shop_id, name, quantity, price, status)
SELECT id, 'Macbook Air M2', 5, 25000000.00, 'ACTIVE' FROM shops WHERE name = 'Tiệm Đồ Điện Tử ABC';

-- 1. Macbook Air M2 thuộc 3 danh mục: Điện thoại (tạm gọi là thiết bị di động), Đồ công nghệ, Hàng cao cấp
INSERT INTO product_categories (product_id, category_id)
SELECT p.id, c.id FROM products p, categories c 
WHERE p.name = 'Macbook Air M2' 
AND c.name IN ('Điện thoại', 'Đồ công nghệ', 'Hàng cao cấp');

-- 2. Bàn phím AKKO thuộc 2 danh mục: Gaming, Thiết bị làm việc
INSERT INTO product_categories (product_id, category_id)
SELECT p.id, c.id FROM products p, categories c 
WHERE p.name = 'Bàn phím cơ AKKO' 
AND c.name IN ('Gaming', 'Thiết bị làm việc');

-- 3. Chuột Logitech thuộc 2 danh mục: Gaming, Đồ công nghệ
INSERT INTO product_categories (product_id, category_id)
SELECT p.id, c.id FROM products p, categories c 
WHERE p.name = 'Chuột Logitech G502' 
AND c.name IN ('Gaming', 'Đồ công nghệ');

-- 4. iPhone 15 Pro Max (đã tạo ở trước) thêm vào danh mục: Hàng cao cấp, Đồ công nghệ
INSERT INTO product_categories (product_id, category_id)
SELECT p.id, c.id FROM products p, categories c 
WHERE p.name = 'iPhone 15 Pro Max' 
AND c.name IN ('Hàng cao cấp', 'Đồ công nghệ');