CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    merchant_key VARCHAR(255) NOT NULL,
    expected_amount DECIMAL(15,2) NOT NULL,
    category_id BIGINT,
    credit_card_id BIGINT,
    billing_cycle VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    start_date DATE,
    last_detected_date DATE,
    last_detected_amount DECIMAL(15,2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_subscription_owner FOREIGN KEY (owner_id) REFERENCES users(id),
    CONSTRAINT fk_subscription_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_subscription_card FOREIGN KEY (credit_card_id) REFERENCES credit_cards(id)
);

CREATE INDEX idx_subscription_owner_active ON subscriptions (owner_id, active);
CREATE UNIQUE INDEX idx_subscription_owner_merchant ON subscriptions (owner_id, merchant_key, active);
