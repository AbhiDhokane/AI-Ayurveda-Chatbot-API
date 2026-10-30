package com.ayurveda.bot.model;

import java.time.format.DateTimeFormatter;

public class ChatMessageDto {
    private Long id;
    private String sender;
    private String content;
    private String language;
    private String timestamp;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy · hh:mm a");

    public ChatMessageDto() {}

    public ChatMessageDto(ChatMessage message) {
        this.id = message.getId();
        this.sender = message.getSender();
        this.content = message.getContent();
        this.language = message.getLanguage();
        if (message.getCreatedAt() != null) {
            this.timestamp = message.getCreatedAt().format(FORMATTER);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
