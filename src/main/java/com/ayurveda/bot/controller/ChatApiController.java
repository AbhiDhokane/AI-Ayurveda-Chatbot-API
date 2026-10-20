package com.ayurveda.bot.controller;

import com.ayurveda.bot.model.ChatRequest;
import com.ayurveda.bot.model.ChatResponse;
import com.ayurveda.bot.service.AiChatService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatApiController {

    private final AiChatService aiChatService;

    public ChatApiController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    // ============================================================
    // NORMAL POST ENDPOINT
    // ============================================================

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest request, HttpSession session) {

        if (session.getAttribute(AuthController.USER_ID) == null) {
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
            StringBuilder answer = new StringBuilder();
            aiChatService.streamChat(
                    language,
                    request.getMessage(),
                    answer::append
            );

            return ResponseEntity.ok(
                    new ChatResponse(
                            answer.toString(),
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
            HttpSession session
    ) {

        if (session.getAttribute(AuthController.USER_ID) == null) {
            SseEmitter unauthorized = new SseEmitter(1000L);
            try {
                unauthorized.send(SseEmitter.event().name("error").data("\"Please log in first.\""));
            } catch (IOException ignored) {}
            unauthorized.complete();
            return unauthorized;
        }

        // Five-minute timeout
        SseEmitter emitter = new SseEmitter(300000L);

        emitter.onTimeout(emitter::complete);
        emitter.onCompletion(() -> System.out.println("SSE completed."));
        emitter.onError(error -> System.out.println("SSE error: " + error.getMessage()));

        Thread thread = new Thread(() -> {
            try {
                aiChatService.streamChat(
                        language,
                        message,
                        chunk -> {
                            try {
                                String jsonChunk = new com.fasterxml.jackson.databind.ObjectMapper()
                                        .writeValueAsString(chunk);

                                emitter.send(
                                        SseEmitter.event().data(jsonChunk)
                                );
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        }
                );

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