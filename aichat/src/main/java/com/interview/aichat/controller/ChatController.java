package com.interview.aichat.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/ai/v1")
public class ChatController {
    //private final

    @RequestMapping("/test")
    public String chatController(){
        return "Hello World";
    }



}
