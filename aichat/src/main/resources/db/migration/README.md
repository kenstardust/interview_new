# 数据库表创建执行方案

## 方法一：通过Nacos获取配置后手动执行（推荐）

### 步骤1：从Nacos获取数据库配置

访问Nacos控制台：http://10.1.40.171:8848/nacos

登录后，在"配置管理" -> "配置列表"中查找：
- **Data ID**: `aichat.yaml` 或 `aichat-dev.yaml`
- **Group**: `DEFAULT_GROUP`
- **命名空间**: `648bf097-1fa4-47cb-a31f-696e1869fb38`

在配置内容中找到数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/interview_db
    username: postgres
    password: your_password
```

### 步骤2：执行SQL脚本

使用数据库连接信息执行SQL脚本：

```bash
# 方式1：使用psql命令行工具
psql -h localhost -U postgres -d interview_db -p 5432 -f aichat/src/main/resources/db/migration/V1__Create_RAG_Tables.sql

# 方式2：使用pgAdmin或其他PostgreSQL客户端工具
# 连接数据库后，打开SQL脚本文件并执行
```

---

## 方法二：创建Spring Boot启动时自动执行（推荐）

### 修改application.yaml配置

在Nacos配置中心添加Flyway配置：

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
```

### 添加Flyway依赖

在 `aichat/pom.xml` 中添加：

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

启动Spring Boot应用时，Flyway会自动执行SQL脚本。

---

## 方法三：创建独立的SQL执行工具

### 创建Java工具类执行SQL脚本

```java
package com.interview.aichat.util;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

    private final DataSource dataSource;

    public DatabaseMigrationRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db/migration/V1__Create_RAG_Tables.sql"));
        populator.execute(dataSource);
        System.out.println("数据库表创建成功！");
    }
}
```

---

## 方法四：使用Docker PostgreSQL容器（测试环境）

如果您有Docker环境，可以快速启动PostgreSQL并执行SQL：

```bash
# 1. 启动PostgreSQL容器
docker run --name rag-postgres \
  -e POSTGRES_DB=interview_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  -d postgres:15

# 2. 执行SQL脚本
docker exec -i rag-postgres psql -U postgres -d interview_db < aichat/src/main/resources/db/migration/V1__Create_RAG_Tables.sql
```

---

## 验证表是否创建成功

执行以下SQL验证：

```sql
-- 查看所有表
SELECT tablename FROM pg_tables WHERE schemaname = 'public';

-- 查看pgvector扩展是否已安装
SELECT * FROM pg_extension WHERE extname = 'vector';

-- 查看document_chunk表的向量索引
SELECT indexname, indexdef FROM pg_indexes WHERE tablename = 'document_chunk';

-- 预期输出：
-- 1. chat_conversation
-- 2. chat_message
-- 3. document_chunk
-- 4. message_file
-- 5. pgvector扩展已安装
-- 6. idx_chunk_embedding索引已创建
```

---

## 如果遇到问题

### 问题1：pgvector扩展未安装

错误信息：`ERROR: could not open extension control file`

解决方案：
```sql
-- 在PostgreSQL中安装pgvector扩展
CREATE EXTENSION vector;
```

如果提示找不到扩展，需要先安装pgvector：
```bash
# Ubuntu/Debian
sudo apt-get install postgresql-15-pgvector

# macOS
brew install pgvector

# Docker（使用已包含pgvector的镜像）
docker pull pgvector/pgvector:pg15
```

### 问题2：外键约束错误

错误信息：`ERROR: relation "chatfile" does not exist`

原因：chatfile表不存在

解决方案：
```sql
-- 先创建chatfile表（如果不存在）
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

-- 然后再执行V1__Create_RAG_Tables.sql
```

### 问题3：权限不足

错误信息：`ERROR: permission denied`

解决方案：
```sql
-- 授予当前用户创建表的权限
GRANT ALL PRIVILEGES ON DATABASE interview_db TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA public TO postgres;
```

---

## 推荐执行顺序

1. **首选方法一**：从Nacos获取配置，使用psql手动执行（最安全、可控）
2. **次选方法二**：配置Flyway自动执行（最规范、可追溯）
3. **测试环境**：使用Docker快速启动PostgreSQL（最便捷）

您希望使用哪种方法？我可以为您提供更详细的执行指导。