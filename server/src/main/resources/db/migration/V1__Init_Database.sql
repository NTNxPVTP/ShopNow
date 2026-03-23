CREATE TYPE user_role AS ENUM ('SELLER', 'CUSTOMER', 'ADMIN');
CREATE TYPE product_status AS ENUM ('ACTIVE', 'SOLD');
CREATE TYPE order_status AS ENUM ('IN_PROCESS', 'DELIVERING', 'PAID');
CREATE TYPE payment_status AS ENUM ('NOT_PAID', 'PAID');

-- 1. Users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role user_role DEFAULT 'CUSTOMER',
    avatar_url TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tokens
CREATE TABLE tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    token TEXT UNIQUE NOT NULL,
    token_type VARCHAR(50),
    expired BOOLEAN DEFAULT false,
    revoked BOOLEAN DEFAULT false
);

-- 3. Shops
CREATE TABLE shops (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    address TEXT,
    avatar_url TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 4. Categories
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) UNIQUE NOT NULL
);

-- 5. Products
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id UUID REFERENCES shops(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    picture_url TEXT,
    quantity INTEGER DEFAULT 0,
    price DECIMAL(12, 2) NOT NULL,
    status product_status DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 6. Product Categories
CREATE TABLE product_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID REFERENCES products(id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories(id) ON DELETE CASCADE
);

-- 7. Reviews
CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reviewer_id UUID REFERENCES users(id),
    product_id UUID REFERENCES products(id) ON DELETE CASCADE,
    content TEXT,
    rating INTEGER CHECK (rating >= 1 AND rating <= 5),
    review_reply TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 8. Carts
CREATE TABLE carts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES users(id) ON DELETE CASCADE,
    total_product INTEGER DEFAULT 0,
    total_price DECIMAL(12, 2) DEFAULT 0
);

-- 9. Cart Product
CREATE TABLE cart_product (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id UUID REFERENCES carts(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id) ON DELETE CASCADE,
    quantity INTEGER DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 10. Orders
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES users(id),
    status order_status DEFAULT 'IN_PROCESS',
    total_price DECIMAL(12, 2) NOT NULL,
    address_shipping TEXT NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 11. Order Detail
CREATE TABLE order_detail (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id),
    product_name VARCHAR(255),
    price DECIMAL(12, 2) NOT NULL,
    quantity INTEGER NOT NULL
);

-- 12. Payment Methods
CREATE TABLE payment_method (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    method VARCHAR(50) NOT NULL
);

-- 13. Payments
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID REFERENCES orders(id) ON DELETE CASCADE,
    payment_method_id UUID REFERENCES payment_method(id),
    status payment_status DEFAULT 'NOT_PAID',
    amount DECIMAL(12, 2) NOT NULL,
    transaction_id VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);