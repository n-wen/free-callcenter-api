-- 呼叫中心 MVP 数据库表结构
-- 执行方式: psql -U postgres -d callcenter -f 001-create-tables.sql

-- 分机表
CREATE TABLE IF NOT EXISTS extension (
    id BIGSERIAL PRIMARY KEY,
    extension_number VARCHAR(20) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    display_name VARCHAR(100),
    status VARCHAR(20) DEFAULT 'OFFLINE' NOT NULL,
    context VARCHAR(50) DEFAULT 'default',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_extension_number ON extension(extension_number);
CREATE INDEX IF NOT EXISTS idx_extension_status ON extension(status);

-- IVR菜单表
CREATE TABLE IF NOT EXISTS ivr_menu (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    welcome_sound VARCHAR(200),
    timeout_seconds INT DEFAULT 5,
    max_attempts INT DEFAULT 3,
    invalid_sound VARCHAR(200),
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- IVR选项表
CREATE TABLE IF NOT EXISTS ivr_option (
    id BIGSERIAL PRIMARY KEY,
    ivr_menu_id BIGINT NOT NULL REFERENCES ivr_menu(id) ON DELETE CASCADE,
    digit VARCHAR(10) NOT NULL,
    action VARCHAR(50) NOT NULL,
    destination VARCHAR(200),
    description VARCHAR(200),
    priority INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(ivr_menu_id, digit)
);

CREATE INDEX IF NOT EXISTS idx_ivr_option_menu ON ivr_option(ivr_menu_id);

-- 通话记录表
CREATE TABLE IF NOT EXISTS call_record (
    id BIGSERIAL PRIMARY KEY,
    call_id VARCHAR(100) NOT NULL UNIQUE,
    caller_number VARCHAR(50) NOT NULL,
    callee_number VARCHAR(50) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    status VARCHAR(20) DEFAULT 'INITIATED' NOT NULL,
    start_time TIMESTAMP NOT NULL,
    answer_time TIMESTAMP,
    end_time TIMESTAMP,
    duration_seconds INT DEFAULT 0,
    extension_id BIGINT REFERENCES extension(id),
    recording_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_call_record_call_id ON call_record(call_id);
CREATE INDEX IF NOT EXISTS idx_call_record_caller ON call_record(caller_number);
CREATE INDEX IF NOT EXISTS idx_call_record_callee ON call_record(callee_number);
CREATE INDEX IF NOT EXISTS idx_call_record_start_time ON call_record(start_time DESC);
CREATE INDEX IF NOT EXISTS idx_call_record_extension ON call_record(extension_id);
