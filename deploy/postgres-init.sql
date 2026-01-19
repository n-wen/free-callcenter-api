-- PostgreSQL Docker 初始化脚本
-- 当容器首次启动时，PostgreSQL 会自动执行此脚本

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'free_callcenter') THEN
        CREATE DATABASE free_callcenter;
        RAISE NOTICE 'Database free_callcenter created';
    ELSE
        RAISE NOTICE 'Database free_callcenter already exists';
    END IF;
END $$;
