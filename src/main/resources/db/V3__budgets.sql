CREATE TABLE IF NOT EXISTS budgets (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id    BIGINT       NOT NULL,
    category_id BIGINT       NULL,
    limit_amount DECIMAL(15,2) NOT NULL,
    budget_month VARCHAR(7)   NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL,
    CONSTRAINT fk_budget_owner    FOREIGN KEY (owner_id)    REFERENCES users(id),
    CONSTRAINT fk_budget_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE INDEX idx_budget_owner_active ON budgets (owner_id, active);
