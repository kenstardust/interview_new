# 数据库表自动创建配置指南

## 当前配置状态

✅ 已添加Flyway依赖到 `aichat/pom.xml`
✅ SQL脚本已就绪：`aichat/src/main/resources/db/migration/V1__Create_RAG_Tables.sql`

## 下一步操作步骤

### 步骤1：重新加载Maven依赖

在IDE中重新加载Maven项目：
- **IntelliJ IDEA**：右键点击项目 → Maven → Reload Project
- **Eclipse**：右键点击项目 → Maven → Update Project
- **命令行**：`mvn clean install`

### 步骤2：在Nacos配置中心添加Flyway配置

访问Nacos控制台：http://10.1.40.171:8848/nacos

1. 登录后，进入"配置管理" → "配置列表"
2. 找到Data ID为 `aichat.yaml` 或 `aichat-dev.yaml` 的配置
3. 点击"编辑"，在配置内容中添加以下内容：

```yaml
spring:
  # Flyway数据库迁移配置
  flyway:
    enabled: true                          # 启用Flyway
    locations: classpath:db/migration      # SQL脚本位置
    baseline-on-migrate: true              # 首次运行时创建baseline
    validate-on-migrate: true              # 验证SQL脚本
    table: flyway_schema_history           # 历史记录表名
    baseline-version: 0                    # baseline版本号
    encoding: UTF-8                        # SQL文件编码

  # 确保数据源配置存在
  datasource:
    driver-class-name: org.postgresql.Driver
    # url, username, password 应该已经在配置中
```

### 步骤3：启动应用，自动创建表

重新启动aichat服务：

```bash
# 方式1：使用Maven启动
cd aichat
mvn spring-boot:run

# 方式2：使用IDE启动
# 直接运行 AiChat.java 的 main 方法

# 方式3：使用JAR包启动
java -jar aichat/target/aichat-0.0.1-SNAPSHOT.jar
```

启动日志中会看到类似以下输出：

```
Flyway: Migrating schema "public" to version "1 - Create RAG Tables"
Flyway: Successfully applied 1 migration to schema "public"
```

### 步骤4：验证表是否创建成功

连接到PostgreSQL数据库，执行以下查询：

```sql
-- 查看所有创建的表
SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;

-- 预期输出：
-- chat_conversation
-- chat_message
-- chatfile
-- document_chunk
-- flyway_schema_history
-- message_file

-- 查看pgvector扩展
SELECT * FROM pg_extension WHERE extname = 'vector';

-- 查看向量索引
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'document_chunk';

-- 预期输出：
-- idx_chunk_embedding (HNSW向量索引)
-- idx_chunk_file
-- idx_chunk_index
```

## 如果遇到问题

### 问题1：Flyway找不到SQL脚本

检查：
1. SQL脚本文件名必须以 `V{version}__{description}.sql` 格式命名（注意双下划线）
2. 文件位置：`src/main/resources/db/migration/`
3. Flyway配置中的 `locations` 路径正确

### 问题2：pgvector扩展未安装

错误：`ERROR: could not open extension control file`

解决方案：先在PostgreSQL中安装pgvector扩展

```sql
-- 连接数据库后执行
CREATE EXTENSION IF NOT EXISTS vector;
```

### 问题3：chatfile表不存在

错误：`ERROR: relation "chatfile" does not exist`

原因：V1脚本中的外键引用了chatfile表，但该表尚未创建

解决方案：
1. 检查chatfile表是否已存在
2. 如果不存在，先创建chatfile表：

```sql
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
```

然后重新启动应用让Flyway执行迁移。

### 问题4：表已存在冲突

错误：`ERROR: relation "chat_conversation" already exists`

解决方案：
1. 如果表已存在，Flyway会自动跳过（因为设置了baseline-on-migrate: true）
2. 或者在Nacos配置中设置：

```yaml
spring:
  flyway:
    clean-disabled: false  # 允许清理（仅开发环境！）
```

## 手动执行SQL脚本（备选方案）

如果Flyway自动执行失败，可以手动执行SQL脚本：

```bash
# 从Nacos获取数据库连接信息后执行
psql -h {host} -U {username} -d {database} -p {port} -f aichat/src/main/resources/db/migration/V1__Create_RAG_Tables.sql

# 示例
psql -h localhost -U postgres -d interview_db -p 5432 -f aichat/src/main/resources/db/migration/V1__Create_RAG_Tables.sql
```

## 成功标志

如果一切正常，您会看到：

1. ✅ 应用启动成功，无错误日志
2. ✅ Flyway历史表中记录了V1版本
3. ✅ 5张表创建成功（chat_conversation, chat_message, document_chunk, message_file, chatfile）
4. ✅ pgvector扩展已安装
5. ✅ 向量索引已创建

## 下一步

表创建成功后，可以：

1. 测试API接口（使用Postman或curl）
2. 集成DashScope API（LLM + Embedding）
3. 开发前端界面
4. 端到端测试RAG流程

如有任何问题，请查看应用日志或数据库日志排查。