-- RAG Enterprise Knowledge Base System - Database Schema Migration
-- Version: V1
-- Description: Create tables for conversation management, message storage, and RAG document chunking with vector embeddings

-- Enable pgvector extension (if not already enabled)
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================================================
-- 0. Create chatfile table FIRST (referenced by other tables)
-- ============================================================================
-- Note: chatfile table might already exist, create if not exists
CREATE TABLE IF NOT EXISTS chatfile (
    id BIGSERIAL PRIMARY KEY,
    uploadedat TIMESTAMP,
    originalfilename VARCHAR(255),
    filesize BIGINT,
    accesscount INTEGER,
    contenttype VARCHAR(100),
    storagekey VARCHAR(500),
    filetext TEXT,
    lastaccessedat TIMESTAMP,
    analyzeError TEXT,
    fileHash VARCHAR(100),
    taskstatus VARCHAR(20) DEFAULT 'PENDING'
);

-- Add taskstatus column if table exists but column doesn't
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'chatfile' AND column_name = 'taskstatus'
    ) THEN
        ALTER TABLE chatfile ADD COLUMN taskstatus VARCHAR(20) DEFAULT 'PENDING';
    END IF;
END $$;

-- ============================================================================
-- 1. Conversation Session Table
-- ============================================================================
CREATE TABLE chat_conversation (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(36) UNIQUE NOT NULL,
    title VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    message_count INTEGER DEFAULT 0,
    status INTEGER DEFAULT 1  -- 1=ACTIVE, 2=ARCHIVED, 3=DELETED
);

COMMENT ON TABLE chat_conversation IS '会话表：管理用户对话会话';
COMMENT ON COLUMN chat_conversation.conversation_id IS '会话UUID（对外引用ID）';
COMMENT ON COLUMN chat_conversation.title IS '会话标题（自动生成或用户编辑）';
COMMENT ON COLUMN chat_conversation.status IS '会话状态：1=活跃, 2=归档, 3=已删除';

-- Index for conversation_id lookups
CREATE INDEX idx_conversation_uuid ON chat_conversation(conversation_id);

-- ============================================================================
-- 2. Chat Message Table
-- ============================================================================
CREATE TABLE chat_message (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,  -- 'user', 'assistant', 'system'
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    token_count INTEGER,
    metadata TEXT,  -- JSON: {model, finish_reason, fileIds}
    parent_message_id BIGINT,  -- For message threading (optional)
    FOREIGN KEY (conversation_id) REFERENCES chat_conversation(conversation_id) ON DELETE CASCADE,
    FOREIGN KEY (parent_message_id) REFERENCES chat_message(id) ON DELETE SET NULL
);

COMMENT ON TABLE chat_message IS '消息表：存储对话中的所有消息';
COMMENT ON COLUMN chat_message.role IS '消息角色：user(用户), assistant(AI助手), system(系统)';
COMMENT ON COLUMN chat_message.metadata IS 'JSON元数据：包含模型名称、finish_reason、关联文件等';
COMMENT ON COLUMN chat_message.token_count IS 'Token计数（用于使用统计）';

-- Index for conversation message retrieval
CREATE INDEX idx_message_conversation ON chat_message(conversation_id);
CREATE INDEX idx_message_created_at ON chat_message(created_at);

-- ============================================================================
-- 3. Document Chunk Table (Core RAG Table with Vector Embeddings)
-- ============================================================================
CREATE TABLE document_chunk (
    id BIGSERIAL PRIMARY KEY,
    file_id BIGINT NOT NULL,
    chunk_id VARCHAR(36) UNIQUE NOT NULL,
    content TEXT NOT NULL,
    chunk_index INTEGER NOT NULL,  -- Order in document (0-based)
    start_position INTEGER,  -- Character position in original document
    end_position INTEGER,
    token_count INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    embedding vector(1536),  -- DashScope embedding dimension (1536)
    FOREIGN KEY (file_id) REFERENCES chatfile(id) ON DELETE CASCADE
);

COMMENT ON TABLE document_chunk IS '文档分块表：RAG核心表，存储文档分块及其向量嵌入';
COMMENT ON COLUMN document_chunk.chunk_id IS '分块UUID（对外引用ID）';
COMMENT ON COLUMN document_chunk.chunk_index IS '分块序号（文档内顺序）';
COMMENT ON COLUMN document_chunk.embedding IS '向量嵌入（1536维，DashScope text-embedding-v2）';

-- Vector similarity search index (HNSW algorithm - high recall)
CREATE INDEX idx_chunk_embedding ON document_chunk
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

COMMENT ON INDEX idx_chunk_embedding IS '向量索引：用于相似度检索，m=16（连接数），ef_construction=64（构建质量）';

-- Regular indexes for file chunk retrieval
CREATE INDEX idx_chunk_file ON document_chunk(file_id);
CREATE INDEX idx_chunk_index ON document_chunk(chunk_index);

-- Optional: IVFFlat index for large-scale datasets (>100k vectors)
-- CREATE INDEX idx_chunk_embedding_ivf ON document_chunk
-- USING ivfflat (embedding vector_cosine_ops)
-- WITH (lists = 100);

-- ============================================================================
-- 4. Message-File Association Table
-- ============================================================================
CREATE TABLE message_file (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    attached_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    purpose VARCHAR(50) DEFAULT 'knowledge',  -- 'knowledge', 'reference', 'attachment'
    FOREIGN KEY (message_id) REFERENCES chat_message(id) ON DELETE CASCADE,
    FOREIGN KEY (file_id) REFERENCES chatfile(id) ON DELETE CASCADE
);

COMMENT ON TABLE message_file IS '消息-文件关联表：记录消息引用的文件';
COMMENT ON COLUMN message_file.purpose IS '文件用途：knowledge(知识库), reference(参考资料), attachment(附件)';

-- Indexes for association queries
CREATE INDEX idx_message_file_message ON message_file(message_id);
CREATE INDEX idx_message_file_file ON message_file(file_id);

-- ============================================================================
-- 5. Utility Functions
-- ============================================================================

-- Function to update conversation.updated_at automatically
CREATE OR REPLACE FUNCTION update_conversation_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE chat_conversation
    SET updated_at = CURRENT_TIMESTAMP
    WHERE conversation_id = NEW.conversation_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to call update function on new message
CREATE TRIGGER trigger_update_conversation_timestamp
AFTER INSERT ON chat_message
FOR EACH ROW
EXECUTE FUNCTION update_conversation_timestamp();

-- Function to update conversation.message_count automatically
CREATE OR REPLACE FUNCTION update_message_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE chat_conversation
        SET message_count = message_count + 1
        WHERE conversation_id = NEW.conversation_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE chat_conversation
        SET message_count = message_count - 1
        WHERE conversation_id = OLD.conversation_id;
        RETURN OLD;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Trigger to update message count
CREATE TRIGGER trigger_update_message_count
AFTER INSERT OR DELETE ON chat_message
FOR EACH ROW
EXECUTE FUNCTION update_message_count();

-- ============================================================================
-- 7. Sample Data (Optional - For Testing)
-- ============================================================================

-- Insert a test conversation (optional, can be removed in production)
-- INSERT INTO chat_conversation (conversation_id, title, status)
-- VALUES ('test-conv-001', 'Test Conversation', 1);

-- ============================================================================
-- Migration Notes:
-- ============================================================================
-- 1. pgvector extension must be installed in PostgreSQL before running this script
--    Installation: CREATE EXTENSION vector;
-- 2. chatfile table is created FIRST because other tables reference it via foreign keys
-- 3. HNSW index parameters:
--    - m=16: Number of connections per layer (trade-off between recall and index size)
--    - ef_construction=64: Search range during index construction (higher = better quality)
-- 4. Vector dimension: 1536 (DashScope text-embedding-v2 standard)
-- 5. Foreign key constraints:
--    - ON DELETE CASCADE: Automatically delete related records when parent is deleted
--    - Ensures data consistency
-- 6. Indexes:
--    - HNSW vector index: Optimized for similarity search (top-K queries)
--    - B-tree indexes: Optimized for file_id, conversation_id lookups
-- 7. Triggers:
--    - Automatically update conversation.updated_at when new message is added
--    - Automatically update conversation.message_count
-- ============================================================================