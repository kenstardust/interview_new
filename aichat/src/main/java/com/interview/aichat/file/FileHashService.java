package com.interview.aichat.file;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FileHashService {
    private final String HASH_ALGORITHM = "SHA-256";
    private final int BUFFER_SIZE = 8192;


}
