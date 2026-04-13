package com.industry.aichat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.industry.aichat.mapper.ChatFileMapper;
import com.industry.aichat.model.ChatFile;
import com.industry.aichat.service.ChatFileService;
import org.springframework.stereotype.Service;

@Service
public class ChatFileServiceImpl extends ServiceImpl<ChatFileMapper,ChatFile> implements ChatFileService {
}
