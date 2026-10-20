-- =============================================================================
-- Corporate Employee Expense & Advance Settlement Platform
-- Database Schema Script for MySQL 8.x / MySQL Workbench
-- =============================================================================

CREATE DATABASE IF NOT EXISTS expense_platform 
    DEFAULT CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE expense_platform;

-- 1. Employees Table
CREATE TABLE IF NOT EXISTS employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    employee_code VARCHAR(50) NOT NULL UNIQUE,
    department VARCHAR(100) NOT NULL,
    designation VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'EMPLOYEE',
    manager_id BIGINT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_employee_manager FOREIGN KEY (manager_id) REFERENCES employees(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 2. Expense Policies Table
CREATE TABLE IF NOT EXISTS expense_policies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL,
    max_amount DECIMAL(15, 2) NOT NULL,
    daily_limit DECIMAL(15, 2) NULL,
    requires_receipt BOOLEAN NOT NULL DEFAULT TRUE,
    eligible_employee_types VARCHAR(200) NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    approval_threshold DECIMAL(15, 2) NULL,
    description VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 3. Advance Requests Table
CREATE TABLE IF NOT EXISTS advance_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    purpose VARCHAR(300) NOT NULL,
    requested_amount DECIMAL(15, 2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    category VARCHAR(100) NOT NULL DEFAULT 'Travel',
    department VARCHAR(100) NULL,
    business_activity VARCHAR(200) NULL,
    supporting_info VARCHAR(500) NULL,
    status VARCHAR(25) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_advance_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 4. Approval History Table
CREATE TABLE IF NOT EXISTS approval_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    advance_request_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    decision VARCHAR(20) NOT NULL,
    comments VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_approval_advance FOREIGN KEY (advance_request_id) REFERENCES advance_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_approval_approver FOREIGN KEY (approver_id) REFERENCES employees(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 5. Advance Disbursements Table
CREATE TABLE IF NOT EXISTS advance_disbursements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    advance_request_id BIGINT NOT NULL,
    amount_disbursed DECIMAL(15, 2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    transaction_reference VARCHAR(100) NOT NULL,
    payment_date DATE NOT NULL,
    remarks VARCHAR(500) NULL,
    disbursed_by_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_disbursement_advance FOREIGN KEY (advance_request_id) REFERENCES advance_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_disbursement_user FOREIGN KEY (disbursed_by_id) REFERENCES employees(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 6. Expenses Table
CREATE TABLE IF NOT EXISTS expenses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    advance_request_id BIGINT NULL,
    expense_date DATE NOT NULL,
    category VARCHAR(100) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    description VARCHAR(500) NOT NULL,
    receipt_path VARCHAR(300) NULL,
    business_activity VARCHAR(200) NULL,
    status VARCHAR(15) NOT NULL DEFAULT 'DRAFT',
    validation_notes VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_expense_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_expense_advance FOREIGN KEY (advance_request_id) REFERENCES advance_requests(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 7. Settlements Table
CREATE TABLE IF NOT EXISTS settlements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    advance_request_id BIGINT NOT NULL UNIQUE,
    total_disbursed DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    total_validated_expenses DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    settlement_difference DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    recovery_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    reimbursement_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    total_recovered DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    total_reimbursed DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(25) NOT NULL DEFAULT 'PENDING_CALCULATION',
    calculated_at DATETIME NULL,
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_settlement_advance FOREIGN KEY (advance_request_id) REFERENCES advance_requests(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 8. Recovery Transactions Table
CREATE TABLE IF NOT EXISTS recovery_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    settlement_id BIGINT NOT NULL,
    amount_recovered DECIMAL(15, 2) NOT NULL,
    recovery_date DATE NOT NULL,
    payment_reference VARCHAR(100) NULL,
    remarks VARCHAR(500) NULL,
    recorded_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recovery_settlement FOREIGN KEY (settlement_id) REFERENCES settlements(id) ON DELETE CASCADE,
    CONSTRAINT fk_recovery_recorder FOREIGN KEY (recorded_by) REFERENCES employees(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 9. Exception Records Table
CREATE TABLE IF NOT EXISTS exception_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exception_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    description VARCHAR(500) NOT NULL,
    status VARCHAR(25) NOT NULL DEFAULT 'OPEN',
    investigated_by BIGINT NULL,
    resolution_notes VARCHAR(1000) NULL,
    resolved_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_exception_investigator FOREIGN KEY (investigated_by) REFERENCES employees(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 10. Audit Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_name VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NULL,
    details VARCHAR(1000) NULL,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 11. Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    message VARCHAR(500) NOT NULL,
    type VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    reference_id BIGINT NULL,
    reference_type VARCHAR(50) NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
) ENGINE=InnoDB;
