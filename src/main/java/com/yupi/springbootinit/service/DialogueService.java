package com.yupi.springbootinit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yupi.springbootinit.model.entity.DialogueResponse;
import com.yupi.springbootinit.utils.AiChatUtils;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Data
public class DialogueService {

    private Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void addEmitter(String sessionId, SseEmitter emitter) {
        emitters.put(sessionId, emitter);
    }

    public SseEmitter getEmitter(String sessionId) {
        SseEmitter sseEmitter = emitters.get(sessionId);
        return sseEmitter;
    }

    public void removeEmitter(String sessionId) {
        emitters.remove(sessionId);
    }

    public void sendResponse(String sessionId, DialogueResponse response) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().data(response));
            } catch (IOException e) {
                // Handle exception
            }
        }
    }


    public void handleUserInput(String sessionId, String message ,AICallback callback) {
        CompletableFuture.runAsync(() -> {
            try {
                AiChatUtils.testNonFunctionSSE1(message,callback);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

        });
    }

    // Other methods for handling dialogue logic
}
