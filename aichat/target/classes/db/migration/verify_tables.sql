-- ============================================
-- 验证RAG系统数据库表和索引创建脚本
-- ============================================

-- 1. 验证pgvector扩展是否已安装
SELECT
    'pgvector扩展' AS 检查项,
    CASE
        WHEN COUNT(*) > 0 THEN '✅ 已安装'
        ELSE '❌ 未安装'
    END AS 状态
FROM pg_extension
WHERE extname = 'vector';

-- 2. 验证所有表是否创建成功
SELECT
    '数据表' AS 检查项,
    CASE
        WHEN COUNT(*) >= 5 THEN '✅ 全部创建'
        ELSE '❌ 部分缺失'
    END AS 状态,
    COUNT(*) AS 已创建数量,
    '预期: 5张表 (chat_conversation, chat_message, document_chunk, message_file, chatfile)' AS 说明
FROM pg_tables
WHERE schemaname = 'public'
AND tablename IN ('chat_conversation', 'chat_message', 'document_chunk', 'message_file', 'chatfile');

-- 3. 列出所有创建的表
SELECT
    '表列表' AS 检查项,
    tablename AS 表名,
    '✅ 已创建' AS 状态
FROM pg_tables
WHERE schemaname = 'public'
AND tablename IN ('chat_conversation', 'chat_message', 'document_chunk', 'message_file', 'chatfile')
ORDER BY tablename;

-- 4. 验证document_chunk表的向量索引
SELECT
    '向量索引' AS 检查项,
    indexname AS 索引名,
    '✅ 已创建' AS 状态
FROM pg_indexes
WHERE tablename = 'document_chunk'
AND indexname LIKE '%embedding%';

-- 5. 验证document_chunk表结构（检查vector字段）
SELECT
    'document_chunk表结构' AS 检查项,
    column_name AS 字段名,
    data_type AS 数据类型,
    '✅ 已配置' AS 状态
FROM information_schema.columns
WHERE table_name = 'document_chunk'
AND column_name IN ('id', 'file_id', 'chunk_id', 'content', 'chunk_index', 'embedding')
ORDER BY ordinal_position;

-- 6. 验证触发器是否创建成功
SELECT
    '触发器' AS 检查项,
    trigger_name AS 触发器名,
    event_manipulation AS 触发事件,
    '✅ 已配置' AS 状态
FROM information_schema.triggers
WHERE event_object_table IN ('chat_message', 'chat_conversation');

-- 7. 验证外键约束
SELECT
    '外键约束' AS 检查项,
    tc.table_name AS 表名,
    tc.constraint_name AS 约束名,
    '✅ 已配置' AS 状态
FROM information_schema.table_constraints tc
WHERE tc.constraint_type = 'FOREIGN KEY'
AND tc.table_name IN ('chat_message', 'document_chunk', 'message_file');

-- 8. 统计信息
SELECT
    '统计信息' AS 检查项,
    '数据库配置' AS 类别,
    current_database() AS 数据库名,
    current_user AS 当前用户,
    version() AS PostgreSQL版本;

-- 9. Flyway迁移历史（如果使用Flyway）
SELECT
    'Flyway迁移' AS 检查项,
    version AS 版本,
    description AS 描述,
    installed_on AS 安装时间,
    '✅ 已执行' AS 状态
FROM flyway_schema_history
ORDER BY installed_rank;

-- ============================================
-- 测试插入和向量操作（可选）
-- ============================================

-- 测试1：插入测试会话
INSERT INTO chat_conversation (conversation_id, title, status)
VALUES ('test-conv-001', '测试会话', 1)
ON CONFLICT (conversation_id) DO NOTHING;

-- 测试2：查询测试会话
SELECT '测试会话查询' AS 检查项,
       conversation_id,
       title,
       '✅ 插入查询成功' AS 状态
FROM chat_conversation
WHERE conversation_id = 'test-conv-001';

-- 测试3：向量操作测试（需要pgvector扩展）
-- 注意：这个测试只在pgvector扩展安装成功后才能执行
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        -- 创建临时表测试向量操作
        CREATE TEMP TABLE test_vector (
            id SERIAL PRIMARY KEY,
            embedding vector(1536)
        );

        -- 插入测试向量（全0向量）
        INSERT INTO test_vector (embedding) VALUES (vector_fill(0, 1536));

        -- 查询测试
        PERFORM * FROM test_vector;

        -- 删除临时表
        DROP TABLE test_vector;

        RAISE NOTICE '✅ 向量操作测试成功';
    ELSE
        RAISE NOTICE '❌ pgvector扩展未安装，跳过向量测试';
    END IF;
END $$;

-- ============================================
-- 清理测试数据（可选）
-- ============================================

-- 删除测试会话
-- DELETE FROM chat_conversation WHERE conversation_id = 'test-conv-001';

-- ============================================
-- 最终验证结果
-- ============================================

SELECT
    '最终验证' AS 检查项,
    CASE
        WHEN
            (SELECT COUNT(*) FROM pg_extension WHERE extname = 'vector') > 0
            AND
            (SELECT COUNT(*) FROM pg_tables WHERE schemaname = 'public' AND tablename IN ('chat_conversation', 'chat_message', 'document_chunk', 'message_file', 'chatfile')) >= 5
        THEN '✅ 所有检查通过！RAG系统数据库准备就绪'
        ELSE '❌ 部分检查未通过，请查看上述结果'
    END AS 最终状态;