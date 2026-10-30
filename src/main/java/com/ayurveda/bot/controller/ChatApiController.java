package com.ayurveda.bot.controller;

import com.ayurveda.bot.model.ChatRequest;
import com.ayurveda.bot.model.ChatResponse;
import com.ayurveda.bot.service.AiChatService;
import com.ayurveda.bot.service.ChatHistoryService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatApiController {

    private static final com.fasterxml.jackson.databind.ObjectMapper OBJECT_MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    private final AiChatService aiChatService;
    private final ChatHistoryService chatHistoryService;

    public ChatApiController(AiChatService aiChatService, ChatHistoryService chatHistoryService) {
        this.aiChatService = aiChatService;
        this.chatHistoryService = chatHistoryService;
    }

    // ============================================================
    // CHAT HISTORY ENDPOINTS
    // ============================================================

    @GetMapping("/history")
    public ResponseEntity<?> getChatHistory(HttpSession session) {
        Long userId = (Long) session.getAttribute(AuthController.USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(ChatResponse.error("Please log in first."));
        }
        return ResponseEntity.ok(chatHistoryService.getHistory(userId));
    }

    @DeleteMapping("/history")
    public ResponseEntity<?> clearChatHistory(HttpSession session) {
        Long userId = (Long) session.getAttribute(AuthController.USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(ChatResponse.error("Please log in first."));
        }
        chatHistoryService.clearHistory(userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Chat history cleared successfully."));
    }

    // ============================================================
    // NORMAL POST ENDPOINT
    // ============================================================

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest request, HttpSession session) {

        Long userId = (Long) session.getAttribute(AuthController.USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(ChatResponse.error("Please log in first."));
        }

        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(ChatResponse.error("Please enter a question."));
        }

        String language = request.getLanguage();
        if (language == null || language.isBlank()) {
            language = "English";
        }

        try {
            // Save user message to database
            chatHistoryService.saveMessage(userId, "user", request.getMessage(), language);

            StringBuilder answer = new StringBuilder();
            aiChatService.streamChat(
                    language,
                    request.getMessage(),
                    answer::append
            );

            String finalAnswer = answer.toString();
            // Save bot response to database
            chatHistoryService.saveMessage(userId, "bot", finalAnswer, language);

            return ResponseEntity.ok(
                    new ChatResponse(
                            finalAnswer,
                            language,
                            true
                    )
            );

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .internalServerError()
                    .body(ChatResponse.error("Unable to connect to AI Service."));
        }
    }

    // ============================================================
    // TRUE STREAMING ENDPOINT
    // ============================================================

    @GetMapping(
            value = "/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter streamChat(
            @RequestParam(defaultValue = "English") String language,
            @RequestParam String message,
            HttpSession session,
            jakarta.servlet.http.HttpServletResponse response
    ) {

        Long userId = (Long) session.getAttribute(AuthController.USER_ID);
        if (userId == null) {
            SseEmitter unauthorized = new SseEmitter(1000L);
            try {
                unauthorized.send(SseEmitter.event().name("error").data("\"Please log in first.\""));
            } catch (IOException ignored) {}
            unauthorized.complete();
            return unauthorized;
        }

        // Save user message to database
        chatHistoryService.saveMessage(userId, "user", message, language);

        // Disable proxy buffering for Render/Nginx/Cloudflare
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Connection", "keep-alive");

        // Five-minute timeout
        SseEmitter emitter = new SseEmitter(300000L);

        emitter.onTimeout(emitter::complete);
        emitter.onCompletion(() -> System.out.println("SSE completed."));
        emitter.onError(error -> System.out.println("SSE error: " + error.getMessage()));

        // Send instant event to flush HTTP headers past reverse proxies
        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException ignored) {}

        Thread thread = new Thread(() -> {
            StringBuilder fullBotResponse = new StringBuilder();
            try {
                aiChatService.streamChat(
                        language,
                        message,
                        chunk -> {
                            try {
                                fullBotResponse.append(chunk);
                                String jsonChunk = OBJECT_MAPPER.writeValueAsString(chunk);

                                emitter.send(
                                        SseEmitter.event().data(jsonChunk)
                                );
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        }
                );

                // Save bot response to database upon completion
                if (fullBotResponse.length() > 0) {
                    chatHistoryService.saveMessage(userId, "bot", fullBotResponse.toString(), language);
                }

                // Tell frontend that generation finished
                emitter.send(
                        SseEmitter.event()
                                .name("done")
                                .data("\"[DONE]\"")
                );

                emitter.complete();

            } catch (Exception e) {
                e.printStackTrace();
                emitter.completeWithError(e);
            }
        });

        thread.setName("ai-stream-thread");
        thread.start();

        return emitter;
    }
}
