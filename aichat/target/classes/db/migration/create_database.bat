@echo off
REM Windows批处理脚本：创建knowledge_basement数据库

echo ============================================
echo 正在创建数据库 knowledge_basement...
echo ============================================

REM 设置PostgreSQL连接参数（根据您的实际配置修改）
set PGHOST=localhost
set PGPORT=5432
set PGUSER=postgres
set PGPASSWORD=postgres

REM 创建数据库
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -c "CREATE DATABASE knowledge_basement;"

if %ERRORLEVEL% EQU 0 (
    echo ============================================
    echo 数据库创建成功！
    echo ============================================
    echo.
    echo 验证数据库是否存在...
    psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -c "\l" | findstr knowledge_basement
    echo.
    echo 现在可以重新启动应用了。
) else (
    echo ============================================
    echo 数据库创建失败！
    echo ============================================
    echo.
    echo 可能的原因：
    echo 1. PostgreSQL未运行
    echo 2. 用户名或密码错误
    echo 3. 数据库已存在
    echo 4. psql命令不在PATH中
    echo.
    echo 请手动执行以下SQL：
    echo psql -U postgres -c "CREATE DATABASE knowledge_basement;"
)

pause