CREATE TABLE employees (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_code VARCHAR(30) UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    position ENUM('MANAGER','STAFF','MECHANIC') NOT NULL,
    status ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('ADMIN','STAFF') NOT NULL,
    active BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_employee
        FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE TABLE customers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    id_number VARCHAR(50),
    note TEXT,
    blacklisted BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_customer_phone ON customers(phone);

CREATE TABLE motorcycles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plate_number VARCHAR(30) NOT NULL UNIQUE,
    model VARCHAR(100) NOT NULL,
    type ENUM('SCOOTER','SPORT','CRUISER','OTHER'),
    engine_cc INT,
    daily_price DECIMAL(10,2) NOT NULL,
    status ENUM('AVAILABLE','RENTED','MAINTENANCE') DEFAULT 'AVAILABLE',
    note TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE bookings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    customer_id BIGINT NOT NULL,
    motorcycle_id BIGINT NOT NULL,
    handled_by BIGINT NOT NULL,  -- staff who created booking

    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,

    daily_price DECIMAL(10,2) NOT NULL,
    total_days INT NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    deposit DECIMAL(10,2) DEFAULT 0,

    status ENUM('RESERVED','ACTIVE','COMPLETED','CANCELLED') NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_booking_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id),

    CONSTRAINT fk_booking_motorcycle
        FOREIGN KEY (motorcycle_id) REFERENCES motorcycles(id),

    CONSTRAINT fk_booking_employee
        FOREIGN KEY (handled_by) REFERENCES employees(id)
);

CREATE INDEX idx_booking_customer ON bookings(customer_id);
CREATE INDEX idx_booking_motorcycle ON bookings(motorcycle_id);
CREATE INDEX idx_booking_employee ON bookings(handled_by);
CREATE INDEX idx_booking_status ON bookings(status);

CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,

    amount DECIMAL(10,2) NOT NULL,
    payment_type ENUM('DEPOSIT','FULL','EXTRA') NOT NULL,
    method ENUM('CASH','BANK','MOBILE') NOT NULL,
    status ENUM('PAID','REFUNDED') DEFAULT 'PAID',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE INDEX idx_payment_booking ON payments(booking_id);

CREATE TABLE returns (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    booking_id BIGINT NOT NULL UNIQUE,
    handled_by BIGINT NOT NULL, -- staff who processed return

    return_time DATETIME NOT NULL,
    late_fee DECIMAL(10,2) DEFAULT 0,
    damage_fee DECIMAL(10,2) DEFAULT 0,
    note TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_return_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(id),

    CONSTRAINT fk_return_employee
        FOREIGN KEY (handled_by) REFERENCES employees(id)
);