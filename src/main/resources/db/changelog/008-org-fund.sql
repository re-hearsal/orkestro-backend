CREATE TABLE org_fund (
    organization_id BIGINT NOT NULL PRIMARY KEY REFERENCES organization(id) ON DELETE CASCADE,
    balance NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (balance >= 0)
);

CREATE TABLE org_fund_transaction (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
    amount NUMERIC(19, 2) NOT NULL,
    description TEXT,
    performed_by_user_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_org_fund_transaction_org_created ON org_fund_transaction (organization_id, created_at DESC);
