-- Document metadata, source and chunk tables (Step 2.2)

CREATE TABLE IF NOT EXISTS document (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    company_id      BIGINT        NOT NULL COMMENT '所属公司',
    title           VARCHAR(512)  NOT NULL COMMENT '文档标题',
    doc_type        VARCHAR(64)   NOT NULL COMMENT 'FINANCIAL_REPORT/ANNOUNCEMENT/RESEARCH_REPORT',
    file_name       VARCHAR(512)  NOT NULL COMMENT '原始文件名',
    file_ext        VARCHAR(16)   NOT NULL COMMENT '扩展名',
    file_size       BIGINT        NOT NULL COMMENT '字节数',
    storage_path    VARCHAR(1024) NOT NULL COMMENT '存储路径',
    publish_date    DATE                   COMMENT '发布日期',
    process_status  VARCHAR(32)   NOT NULL DEFAULT 'UPLOADED' COMMENT '处理状态',
    error_message   VARCHAR(2000)          COMMENT '失败原因',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       NOT NULL DEFAULT 0,
    KEY idx_document_company_id (company_id),
    KEY idx_document_company_type (company_id, doc_type),
    KEY idx_document_status (process_status),
    KEY idx_document_publish_date (publish_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='研究文档';

CREATE TABLE IF NOT EXISTS document_source (
    id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    document_id  BIGINT        NOT NULL COMMENT '关联文档',
    source_type  VARCHAR(32)   NOT NULL DEFAULT 'UPLOAD' COMMENT 'UPLOAD/URL/MANUAL',
    source_url   VARCHAR(1024)          COMMENT '来源 URL',
    source_name  VARCHAR(256)           COMMENT '来源名称',
    raw_meta     TEXT                   COMMENT 'JSON 扩展元数据',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT       NOT NULL DEFAULT 0,
    UNIQUE KEY uk_document_source_document_id (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档来源';

CREATE TABLE IF NOT EXISTS document_chunk (
    id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    document_id  BIGINT        NOT NULL COMMENT '所属文档',
    company_id   BIGINT        NOT NULL COMMENT '公司维度冗余',
    chunk_index  INT           NOT NULL COMMENT '文档内序号',
    title_path   VARCHAR(1024)          COMMENT '标题路径',
    content      MEDIUMTEXT    NOT NULL COMMENT '片段正文',
    page_no      INT                    COMMENT '页码',
    section      VARCHAR(512)           COMMENT '章节',
    token_count  INT                    COMMENT '预估 token',
    vector_id    VARCHAR(128)           COMMENT '向量库向量 ID',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT       NOT NULL DEFAULT 0,
    KEY idx_chunk_document_id (document_id),
    KEY idx_chunk_company_id (company_id),
    KEY idx_chunk_document_index (document_id, chunk_index),
    KEY idx_chunk_vector_id (vector_id),
	KEY idx_chunk_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档知识片段';

INSERT INTO company (stock_code, name, exchange, industry, status)
SELECT '600519', '贵州茅台', 'SH', '白酒', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM company WHERE stock_code = '600519');
