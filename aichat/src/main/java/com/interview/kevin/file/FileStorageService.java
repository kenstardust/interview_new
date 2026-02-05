package com.interview.kevin.file;

import com.interview.kevin.config.StorageConfigurationProperties;
import com.interview.kevin.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerRetryPolicy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import com.interview.kevin.exception.BusinessException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {
    private final S3Client s3Client;
    private final StorageConfigurationProperties storageConfig;

    /**
     * 上传对话文件
     *
     * @param file
     * @return
     */
    public String uploadFile(MultipartFile file) {
        return uploadFileActor(file, "file");
    }

    /**
     * 下载文件
     *
     * @param fileKey
     * @return
     */
    public byte[] downloadFile(String fileKey) {
        return downloadFileActor(fileKey);
    }

    public void deleteFile(String fileKey) {
        deleteFileActor(fileKey);
    }

    /**
     * 下载文件实现
     *
     * @param fileKey
     * @return
     */
    private byte[] downloadFileActor(String fileKey) {
        if (!fileExists(fileKey)) {
            throw new BusinessException(ErrorCode.STORAGE_DOWNLOAD_FAILED, "文件不存在： " + fileKey);
        }

        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(storageConfig.getBucket())
                    .key(fileKey)
                    .build();
            return s3Client.getObjectAsBytes(getRequest).asByteArray();
        } catch (S3Exception e) {
            log.error("下载文件失败： {} - {} ", fileKey, e.getMessage(), e);
            throw new BusinessException(ErrorCode.STORAGE_DOWNLOAD_FAILED, "文件下载失败  :" + e.getMessage());
        }
    }

    /**
     * 检查文件是否存在于容器中
     *
     * @param fileKey
     * @return
     */
    public boolean fileExists(String fileKey) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(storageConfig.getBucket())
                    .key(fileKey)
                    .build();
            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        } catch (S3Exception e) {
            log.warn("文件不存在: {} - {}", fileKey, e.getMessage());
            return false;
        }
    }

    public boolean ensureBucketExists(){
        try{
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(storageConfig.getBucket())
                    .build();
            s3Client.headObject(headRequest);
            log.info("文件桶存在: {}", storageConfig.getBucket());
            return true;
        }catch (S3Exception e){
            log.error("检查失败或者不存在 ：{}", e.getMessage());
        }
        return false;
    }


    private void deleteFileActor(String fileKey) {
        if (fileKey == null || fileKey.isEmpty()) {
            log.debug("文件键为空，跳过删除");
            return;
        }

        //检查文件是否存在
        if (!fileExists(fileKey)) {
            log.warn("文件不存在，跳过删除: {} ", fileKey);
            return;
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(storageConfig.getBucket())
                    .key(fileKey)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("文件删除成功： {} ", fileKey);
        } catch (S3Exception e) {
            log.error("文件删除失败： {} - {}", fileKey, "文件删除失败： " + e.getMessage());
        }
    }

    /**
     * 上传文件实现
     *
     * @param file
     * @param prefix
     * @return
     */
    private String uploadFileActor(MultipartFile file, String prefix) {
        String originalFilename = file.getOriginalFilename();
        String fileKey = generateFileKey(originalFilename, prefix);
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(storageConfig.getBucket())
                    .key(fileKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("文件上传成功：{}->{}", originalFilename, fileKey);
            return fileKey;
        } catch (IOException e) {
            log.error("读取上传文件失败： {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAILED, "文件读取失败");
        } catch (S3Exception e) {
            log.error("上传文件到容器失败： {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAILED, "文件存储失败" + e.getMessage());
        }

    }


    private String generateFileKey(String originalFilename, String prefix) {
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String safeName = sanitizeFilename(originalFilename);
        return String.format("%s/%s/%s/%s", datePath, prefix, uuid, safeName);
    }

    private String sanitizeFilename(String filename) {
        if (filename == null)
            return "unknown";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

}
