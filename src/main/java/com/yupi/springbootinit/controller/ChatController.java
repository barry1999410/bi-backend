package com.yupi.springbootinit.controller;

import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.ResultUtils;
import com.yupi.springbootinit.constant.AiChatConstant;
import com.yupi.springbootinit.model.entity.DialogueResponse;
import com.yupi.springbootinit.service.AICallback;
import com.yupi.springbootinit.service.DialogueService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Resource
    private DialogueService dialogueService;


    @GetMapping ("/sse")
    public SseEmitter handleSse(HttpServletRequest httpServletRequest) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);//长连接
        this.dialogueService.addEmitter(AiChatConstant.Ai_CHAT_id, emitter);
        return emitter;
    }

    @PostMapping("/sse/ai")
    public BaseResponse<String> ChatWithAi(HttpServletRequest httpServletRequest, @RequestBody String message) {
        dialogueService.handleUserInput(AiChatConstant.Ai_CHAT_id, message, new AICallback() {
            @Override
            public void onDataReceived(DialogueResponse response) {
                try {
                    SseEmitter emitter = dialogueService.getEmitter(AiChatConstant.Ai_CHAT_id);
                    if(emitter != null) {
                        emitter.send(SseEmitter.event().data(response));
                    }
                } catch (IOException e) {
                    // 处理发送时的异常
                }
            }

            @Override
            public void onComplete() {
                // 当 AI 模型完成全部响应时调用
            }

            @Override
            public void onError(Exception e) {
                // 处理错误
            }
        });

        return ResultUtils.success("success");
    }


}

