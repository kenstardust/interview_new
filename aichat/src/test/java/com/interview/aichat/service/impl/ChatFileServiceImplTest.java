package com.interview.aichat.service.impl;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import com.interview.aichat.AiChat;
import com.interview.aichat.model.ChatFile;
import com.interview.aichat.service.ChatFileService;
import com.interview.kevin.constant.TaskStatusCode;
import org.apache.ibatis.javassist.tools.rmi.Sample;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AiChat.class)
class ChatFileServiceImplTest {
    @Autowired
    private ChatFileService chatFileService;

    @Test
    public void insert() {
        ChatFile file = ChatFile.builder()
                .originalfilename("张三_简历.pdf")
                .filesize(256000L)
                .contenttype("application/pdf")
                .storagekey("resume/2024/06/zs_abc123")
                .filetext("张三，10年Java经验，精通Spring Cloud...")
                .uploadedat(LocalDateTime.now())
                .lastaccessedat(LocalDateTime.now())
                .taskstatus(TaskStatusCode.valueOf("PENDING")) // ✅ 直接字符串
                .analyzeError(null)
                .fileHash("a1b2c3d4e5f67890" + System.currentTimeMillis()) // 保证唯一
                .build();
        boolean isSuccess = chatFileService.save(file);
        System.out.println("isSuccess: " + isSuccess);

    }

}