package com.interview.aichat.file;

import com.interview.aichat.AiChat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.services.s3.S3Client;

@SpringBootTest(classes = AiChat.class)
class FileStorageServiceTest {

    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private S3Client s3Client;


    @Test
    void uploadChatFile() {

    }

    @Test
    void downloadFile() {
        if(fileStorageService.downloadFile("GBT+39469-2020.pdf")!=null){
            System.out.println("成功");
            byte[] bytes = fileStorageService.downloadFile("GBT+39469-2020.pdf");
            if(bytes.length>0){
                System.out.println("对的对的");
            }
        }
        else{
            System.out.println("失败");
        }
    }

    @Test
    void deleteFile() {
    }

    @Test
    void fileExists() {
    }

    @Test
    void ensureBucketExists() {
    }
}