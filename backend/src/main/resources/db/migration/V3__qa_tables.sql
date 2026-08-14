-- Q&A records and citations (Step 3)

CREATE TABLE IF NOT EXISTS question (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    company_id      BIGINT                 COMMENT '目标公司（可空）',
    question_text   TEXT          NOT NULL COMMENT '用户问题',
    answer_text     MEDIUMTEXT             COMMENT '模型回答',
    intent_type     VARCHAR(32)   NOT NULL DEFAULT 'QA' COMMENT 'QA/SUMMARY/COMPARE/AGENT',
    latency_ms      BIGINT                 COMMENT '总耗时毫秒',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT       NOT NULL DEFAULT 0,
    KEY idx_question_company_id (company_id),
    KEY idx_question_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答记录';

CREATE TABLE IF NOT EXISTS citation (
    id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ref_type     VARCHAR(32)   NOT NULL COMMENT 'QUESTION/SUMMARY',
    ref_id       BIGINT        NOT NULL COMMENT '关联问答或摘要 ID',
    chunk_id     BIGINT        NOT NULL COMMENT '引用片段',
    quote_text   VARCHAR(2000)          COMMENT '引用摘录',
    rank_no      INT           NOT NULL DEFAULT 1 COMMENT '展示顺序',
    score        DOUBLE                 COMMENT '相关分',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      TINYINT       NOT NULL DEFAULT 0,
    KEY idx_citation_ref (ref_type, ref_id),
    KEY idx_citation_chunk_id (chunk_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='引用来源';
