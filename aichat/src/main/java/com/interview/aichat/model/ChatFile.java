package com.interview.aichat.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor // ✅ 必须添加！解决"无法访问构造函数"问题
@AllArgsConstructor
@TableName("chatfile")
public class ChatFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDateTime uploadedat;

    private String originalfilename;

    private Long filesize;

    private Integer accesscount;

    private String contenttype;

    private String storagekey;

    private String filetext;

    private LocalDateTime lastaccessedat;

    /**
     * 任务状态：PENDING, PROCESSING, COMPLETED, FAILED
     * 数据库字段（VARCHAR(20)）
     */
    private String taskstatus;

    private String analyzeError;

    private String fileHash;

    protected void onCreate() {
        uploadedat = LocalDateTime.now();
        lastaccessedat = LocalDateTime.now();
        accesscount = 1;
    }

}
