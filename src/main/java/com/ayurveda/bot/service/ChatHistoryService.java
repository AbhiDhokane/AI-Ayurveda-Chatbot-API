package com.ayurveda.bot.service;

import com.ayurveda.bot.model.ChatMessage;
import com.ayurveda.bot.model.ChatMessageDto;
import com.ayurveda.bot.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatHistoryService {

    private final ChatMessageRepository chatMessageRepository;

    public ChatHistoryService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public ChatMessage saveMessage(Long userId, String sender, String content, String language) {
        if (userId == null || content == null || content.trim().isEmpty()) {
            return null;
        }
        ChatMessage msg = new ChatMessage(userId, sender, content.trim(), language, LocalDateTime.now());
        return chatMessageRepository.save(msg);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getHistory(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return chatMessageRepository.findByUserIdOrderByCreatedAtAsc(userId)
                .stream()
                .map(ChatMessageDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void clearHistory(Long userId) {
        if (userId != null) {
            chatMessageRepository.deleteByUserId(userId);
        }
    }
}
