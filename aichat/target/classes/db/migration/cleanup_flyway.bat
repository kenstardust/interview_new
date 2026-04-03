@echo off
REM Flyway修复脚本：清理失败的迁移记录

echo ============================================
echo Flyway 修复脚本
echo ============================================
echo.
echo 此脚本将：
echo 1. 删除Flyway历史表（flyway_schema_history）
echo 2. 删除部分创建的表
echo 3. 允许重新执行迁移
echo.
pause

REM 设置数据库连接参数
set PGHOST=localhost
set PGPORT=5432
set PGUSER=postgres
set PGPASSWORD=postgres
set PGDATABASE=knowledge_basement

echo.
echo 步骤1：删除Flyway历史表...
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %PGDATABASE% -c "DROP TABLE IF EXISTS flyway_schema_history CASCADE;"

echo.
echo 步骤2：删除部分创建的表（按依赖顺序反向删除）...
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %PGDATABASE% -c "DROP TABLE IF EXISTS message_file CASCADE;"
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %PGDATABASE% -c "DROP TABLE IF EXISTS document_chunk CASCADE;"
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %PGDATABASE% -c "DROP TABLE IF EXISTS chat_message CASCADE;"
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %PGDATABASE% -c "DROP TABLE IF EXISTS chat_conversation CASCADE;"

echo.
echo 步骤3：验证表已删除...
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %PGDATABASE% -c "\dt" | findstr /C:"chat_" /C:"document_" /C:"message_" /C:"flyway_"

echo.
echo ============================================
echo 清理完成！
echo ============================================
echo.
echo 现在可以重新启动应用，Flyway将重新执行迁移。
echo.
pause