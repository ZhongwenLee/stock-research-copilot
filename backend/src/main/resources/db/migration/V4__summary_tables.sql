-- Research summary records and citations (Step 4)

CREATE TABLE IF NOT EXISTS summary (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    company_id      BIGINT        NOT NULL COMMENT '目标公司',
    stock_code      VARCHAR(32)            COMMENT '股票代码冗余',
    mode            VARCHAR(32)   NOT NULL DEFAULT 'FAST' COMMENT 'FAST/DEEP',
    title           VARCHAR(512)  NOT NULL COMMENT '摘要标题',
    overview        MEDIUMTEXT             COMMENT '摘要总览',
    sections_json   MEDIUMTEXT             COMMENT '结构化章节 JSON',
    doc_types_json  VARCHAR(512)           COMMENT '文档类型过滤 JSON',
    start_date      DATE                   COMMENT '筛选起始日期',
    end_date        DATE                   COMMENT '筛选结束日期',
    latency_ms      BIGINT                 COMMENT '总耗时毫秒',
    status          VARCHAR(32)   NOT NULL DEFAULT 'DONE' COMMENT 'DONE/FAILED',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT       NOT NULL DEFAULT 0,
    KEY idx_summary_company_id (company_id),
    KEY idx_summary_created_at (created_at),
    KEY idx_summary_mode (mode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='研究摘要记录';
