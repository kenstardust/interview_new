# interview_new

AiAgent 知识库问答项目，基于 Spring Boot 3 构建。

## 已实现架构

### 1. Redis Stream 两层异步消费架构
- Stage-1（`kb:stream:ingest`）：快速接收请求，立即返回 `ACCEPTED`。
- Stage-2（`kb:stream:generation`）：Consumer Group 并发处理 LLM 生成。
- 每个实例使用独立 consumer 名称，支持横向扩容。

### 2. S3 协议统一存储抽象
- `ObjectStorage` 统一接口。
- `LocalObjectStorage` 用于本地开发。
- `S3ObjectStorage` 兼容 RustFS/MinIO/AWS S3（S3 协议）。
- `UnifiedStorageService` 支持通过配置平滑切换后端。

### 3. RAG 检索增强
- `AliyunEmbeddingClient` 对接阿里 Embedding 模型。
- `PgVectorRetrievalService` 使用 pgvector 进行语义检索。
- `V1__kb_rag_and_drawing.sql` 提供 HNSW 索引初始化。

### 4. 参数化提示词工程
- `PromptRenderingService` 从 classpath `prompts/*.st` 加载模板。
- 使用 `PromptTemplate` 动态变量填充。
- 使用 `BeanOutputConverter` 注入 JSON 结构化输出格式约束。

### 5. 绘画生命周期管理
- `DrawingSessionLifecycleService` 采用 Redis 热缓存 + PostgreSQL 冷存储。
- 提供会话状态机（INIT/GENERATING/WAITING_USER/COMPLETED/FAILED）。
- 支持会话断点续答恢复接口。

## API

- `POST /ai/v1/knowledge/ask`：提交知识库问答任务（异步）。
- `GET /ai/v1/knowledge/drawing/{sessionId}/resume`：恢复会话状态。

## 配置项

```yaml
kb:
  storage:
    backend: LOCAL # LOCAL / S3
    local-path: /tmp/kb-storage
```

环境变量示例：
- `REDIS_HOST`, `REDIS_PORT`
- `POSTGRES_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `KB_STORAGE_BACKEND`, `KB_LOCAL_STORAGE_PATH`
