# interview_new

AiAgent 知识库问答项目，基于 Spring Boot 3 构建。

## 已实现架构

### 1. Redis Stream 两层异步消费架构
- Stage-1（`kb:stream:ingest`）：快速接收请求，立即返回 `ACCEPTED`。
- Stage-2（`kb:stream:generation`）：Consumer Group 并发处理 LLM 生成。
- 每个实例使用独立 consumer 名称，支持横向扩容。

### 2. 统一存储能力（已合并到 `config` + `file` 包）
- `StorageConfigurationProperties` 负责存储参数与模式切换配置。
- `FileStorageService` 负责 S3 协议存储（可对接 RustFS/MinIO/AWS S3）。
- `LocalFileStorageService` 提供本地文件存储能力。
- `UnifiedFileStorageService` 根据 `aichat.storage.mode` 在本地/S3 之间切换。

### 3. RAG 检索增强
- `AliyunEmbeddingClient` 对接阿里 Embedding 模型。
- `PgVectorRetrievalService` 使用 pgvector 进行语义检索。
- `V1__kb_rag_and_drawing.sql` 提供 HNSW 索引初始化。

### 4. 参数化提示词工程
- `PromptRenderingService` 从 classpath `prompts/*.st` 加载模板。
- 使用 `PromptTemplate` 动态变量填充。
- 使用 `BeanOutputConverter` 注入 JSON 结构化输出格式约束。

### 5. 会话生命周期管理
- `DrawingSessionLifecycleService` 采用 Redis 热缓存 + PostgreSQL 冷存储。
- 提供会话状态机（INIT/GENERATING/WAITING_USER/COMPLETED/FAILED）。
- 支持会话断点续答恢复接口。

## API

- `POST /ai/v1/knowledge/ask`：提交知识库问答任务（异步）。
- `GET /ai/v1/knowledge/drawing/{sessionId}/resume`：恢复会话状态。

## 配置项

```yaml
aichat:
  storage:
    mode: S3 # S3 / LOCAL
    local-base-path: /tmp/aichat-storage
    endpoint: http://127.0.0.1:9000
    bucket: aichat
```

环境变量示例：
- `REDIS_HOST`, `REDIS_PORT`
- `POSTGRES_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `AICHAT_STORAGE_MODE`, `AICHAT_LOCAL_STORAGE_PATH`
- `AICHAT_STORAGE_ENDPOINT`, `AICHAT_STORAGE_ACCESS_KEY`, `AICHAT_STORAGE_SECRET_KEY`, `AICHAT_STORAGE_BUCKET`
