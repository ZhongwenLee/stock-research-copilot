-- Flyway baseline schema for infrastructure readiness.
-- Document / chunk tables will be refined in Step 2.2.

CREATE TABLE IF NOT EXISTS company (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    stock_code    VARCHAR(32)  NOT NULL COMMENT '股票代码',
    name          VARCHAR(128) NOT NULL COMMENT '公司名称',
    exchange      VARCHAR(16)           COMMENT '交易所',
    industry      VARCHAR(128)          COMMENT '行业',
    status        VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_company_stock_code (stock_code),
    KEY idx_company_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司';
