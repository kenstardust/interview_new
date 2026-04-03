package com.interview.aichat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interview.aichat.mapper.ChatFileMapper;
import com.interview.aichat.model.ChatFile;
import com.interview.aichat.service.ChatFileService;
import org.springframework.stereotype.Service;

@Service
public class ChatFileServiceImpl extends ServiceImpl<ChatFileMapper,ChatFile> implements ChatFileService {
}
