package com.interview.aichat.config;

import com.pgvector.PGvector;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * pgvector类型处理器：处理Java float[]与PostgreSQL vector类型的转换
 *
 * 用于MyBatis-Plus操作document_chunk表的embedding字段
 *
 * 工作原理：
 * 1. Java -> PostgreSQL: float[] -> PGvector -> SQL参数
 * 2. PostgreSQL -> Java: PGobject -> PGvector -> float[]
 *
 * 依赖：com.pgvector:pgvector库
 */
@MappedTypes(float[].class)
public class PgVectorTypeHandler extends BaseTypeHandler<float[]> {

    /**
     * 设置非空参数：Java float[] -> PostgreSQL vector
     *
     * @param ps        PreparedStatement
     * @param i         参数索引
     * @param parameter float[]数组
     * @param jdbcType  JDBC类型
     * @throws SQLException SQL异常
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, float[] parameter, JdbcType jdbcType) throws SQLException {
        // 创建PGvector对象
        PGvector vector = new PGvector(parameter);
        // 设置为PGobject
        ps.setObject(i, vector);
    }

    /**
     * 根据列名获取可空结果：PostgreSQL vector -> Java float[]
     *
     * @param rs         ResultSet
     * @param columnName 列名
     * @return float[]数组
     * @throws SQLException SQL异常
     */
    @Override
    public float[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        // 获取PGobject
        Object object = rs.getObject(columnName);
        return parseVector(object);
    }

    /**
     * 根据列索引获取可空结果：PostgreSQL vector -> Java float[]
     *
     * @param rs          ResultSet
     * @param columnIndex 列索引
     * @return float[]数组
     * @throws SQLException SQL异常
     */
    @Override
    public float[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Object object = rs.getObject(columnIndex);
        return parseVector(object);
    }

    /**
     * 从CallableStatement获取可空结果：PostgreSQL vector -> Java float[]
     *
     * @param cs          CallableStatement
     * @param columnIndex 列索引
     * @return float[]数组
     * @throws SQLException SQL异常
     */
    @Override
    public float[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Object object = cs.getObject(columnIndex);
        return parseVector(object);
    }

    /**
     * 解析PostgreSQL vector对象为Java float[]
     *
     * 支持两种格式：
     * 1. PGvector对象（com.pgvector.PGvector）
     * 2. PGobject对象（org.postgresql.util.PGobject，类型为"vector")
     *
     * @param object PostgreSQL对象
     * @return float[]数组，如果为null或解析失败则返回null
     */
    private float[] parseVector(Object object) {
        if (object == null) {
            return null;
        }

        try {
            // 如果是PGvector对象，直接转换
            if (object instanceof PGvector) {
                PGvector pgVector = (PGvector) object;
                return pgVector.toArray();
            }

            // 如果是PGobject对象，需要手动解析字符串
            if (object instanceof PGobject) {
                PGobject pgObject = (PGobject) object;
                if ("vector".equals(pgObject.getType())) {
                    // 解析字符串格式：[0.1, 0.2, 0.3, ...]
                    String value = pgObject.getValue();
                    return parseVectorString(value);
                }
            }

            // 不支持的类型
            return null;

        } catch (Exception e) {
            // 解析失败，返回null（避免影响查询）
            return null;
        }
    }

    /**
     * 解析vector字符串为float[]
     *
     * PostgreSQL vector字符串格式：[0.1,0.2,0.3,...]
     *
     * @param value vector字符串
     * @return float[]数组
     */
    private float[] parseVectorString(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        // 移除方括号
        value = value.trim();
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }

        // 分割逗号
        String[] parts = value.split(",");
        float[] result = new float[parts.length];

        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Float.parseFloat(parts[i].trim());
            } catch (NumberFormatException e) {
                // 解析失败，设置为0.0
                result[i] = 0.0f;
            }
        }

        return result;
    }
}