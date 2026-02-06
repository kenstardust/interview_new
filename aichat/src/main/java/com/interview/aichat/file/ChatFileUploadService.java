package com.interview.aichat.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatFileUploadService {

    private final FileStorageService fileStorageService;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;//10MB

    public Map<String,Object> uploadFileAndAnalyze(MultipartFile file) {

        return null;
    }


}
