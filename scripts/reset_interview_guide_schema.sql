-- Manual local reset for database: interview_guide
-- This drops existing local tables and recreates the schema that matches Java fields.

CREATE EXTENSION IF NOT EXISTS vector;

DROP TABLE IF EXISTS message_file CASCADE;
DROP TABLE IF EXISTS document_chunk CASCADE;
DROP TABLE IF EXISTS chat_message CASCADE;
DROP TABLE IF EXISTS chat_conversation CASCADE;
DROP TABLE IF EXISTS chatfile CASCADE;
DROP TABLE IF EXISTS flyway_schema_history CASCADE;
DROP FUNCTION IF EXISTS update_conversation_stats() CASCADE;

CREATE TABLE chatfile (
    id BIGSERIAL PRIMARY KEY,
    uploadedat TIMESTAMP,
    originalfilename VARCHAR(255),
    filesize BIGINT,
    accesscount INTEGER,
    contenttype VARCHAR(100),
    storagekey VARCHAR(500),
    filetext TEXT,
    lastaccessedat TIMESTAMP,
    analyzeerror TEXT,
    filehash VARCHAR(100),
    taskstatus VARCHAR(20) DEFAULT 'PENDING'
);

CREATE TABLE chat_conversation (
    id BIGSERIAL PRIMARY KEY,
    conversationid VARCHAR(36) UNIQUE NOT NULL,
    title VARCHAR(255),
    createdat TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedat TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    messagecount INTEGER DEFAULT 0,
    status INTEGER DEFAULT 1
);

CREATE TABLE chat_message (
    id BIGSERIAL PRIMARY KEY,
    conversationid VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    createdat TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tokencount INTEGER,
    metadata TEXT,
    parentmessageid BIGINT,
    CONSTRAINT fk_message_conversation
        FOREIGN KEY (conversationid)
        REFERENCES chat_conversation(conversationid)
        ON DELETE CASCADE,
    CONSTRAINT fk_parent_message
        FOREIGN KEY (parentmessageid)
        REFERENCES chat_message(id)
        ON DELETE SET NULL
);

CREATE TABLE document_chunk (
    id BIGSERIAL PRIMARY KEY,
    fileid BIGINT NOT NULL,
    chunkid VARCHAR(36) UNIQUE NOT NULL,
    content TEXT NOT NULL,
    chunkindex INTEGER NOT NULL,
    startposition INTEGER,
    endposition INTEGER,
    tokencount INTEGER,
    createdat TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    embedding vector(1536),
    CONSTRAINT fk_chunk_file
        FOREIGN KEY (fileid)
        REFERENCES chatfile(id)
        ON DELETE CASCADE
);

CREATE TABLE message_file (
    id BIGSERIAL PRIMARY KEY,
    messageid BIGINT NOT NULL,
    fileid BIGINT NOT NULL,
    attachedat TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    purpose VARCHAR(50) DEFAULT 'knowledge',
    CONSTRAINT fk_message_file_message
        FOREIGN KEY (messageid)
        REFERENCES chat_message(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_message_file_file
        FOREIGN KEY (fileid)
        REFERENCES chatfile(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_conversation_updatedat
    ON chat_conversation(updatedat DESC);
CREATE INDEX idx_message_conversation
    ON chat_message(conversationid);
CREATE INDEX idx_message_createdat
    ON chat_message(createdat);
CREATE INDEX idx_chunk_file
    ON document_chunk(fileid);
CREATE INDEX idx_chunk_index
    ON document_chunk(chunkindex);
CREATE INDEX idx_message_file_message
    ON message_file(messageid);
CREATE INDEX idx_message_file_file
    ON message_file(fileid);
CREATE INDEX idx_chunk_embedding
    ON document_chunk USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

CREATE OR REPLACE FUNCTION update_conversation_stats()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE chat_conversation
        SET messagecount = COALESCE(messagecount, 0) + 1,
            updatedat = CURRENT_TIMESTAMP
        WHERE conversationid = NEW.conversationid;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE chat_conversation
        SET messagecount = GREATEST(COALESCE(messagecount, 0) - 1, 0),
            updatedat = CURRENT_TIMESTAMP
        WHERE conversationid = OLD.conversationid;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_conversation_stats
AFTER INSERT OR DELETE ON chat_message
FOR EACH ROW
EXECUTE FUNCTION update_conversation_stats();
