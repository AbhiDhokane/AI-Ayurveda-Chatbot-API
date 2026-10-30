package com.ayurveda.bot;

import com.ayurveda.bot.model.ChatMessage;
import com.ayurveda.bot.model.ChatMessageDto;
import com.ayurveda.bot.service.ChatHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ChatHistoryServiceTest {

    @Autowired
    private ChatHistoryService chatHistoryService;

    private final Long testUserId = 9999L;

    @BeforeEach
    void setUp() {
        chatHistoryService.clearHistory(testUserId);
    }

    @Test
    void testSaveAndRetrieveChatHistory() {
        // Initially empty
        List<ChatMessageDto> initial = chatHistoryService.getHistory(testUserId);
        assertTrue(initial.isEmpty());

        // Save a user message
        ChatMessage userMsg = chatHistoryService.saveMessage(testUserId, "user", "What are the benefits of Ashwagandha?", "English");
        assertNotNull(userMsg);
        assertNotNull(userMsg.getId());
        assertEquals("user", userMsg.getSender());

        // Save a bot reply
        ChatMessage botMsg = chatHistoryService.saveMessage(testUserId, "bot", "Ashwagandha (Withania somnifera) is a premier Rasayana herb in Ayurveda.", "English");
        assertNotNull(botMsg);
        assertNotNull(botMsg.getId());
        assertEquals("bot", botMsg.getSender());

        // Retrieve history
        List<ChatMessageDto> history = chatHistoryService.getHistory(testUserId);
        assertEquals(2, history.size());

        assertEquals("user", history.get(0).getSender());
        assertEquals("What are the benefits of Ashwagandha?", history.get(0).getContent());
        assertNotNull(history.get(0).getTimestamp());

        assertEquals("bot", history.get(1).getSender());
        assertTrue(history.get(1).getContent().contains("Ashwagandha"));
    }

    @Test
    void testClearHistory() {
        chatHistoryService.saveMessage(testUserId, "user", "Hello", "English");
        chatHistoryService.saveMessage(testUserId, "bot", "Namaste!", "English");

        assertEquals(2, chatHistoryService.getHistory(testUserId).size());

        chatHistoryService.clearHistory(testUserId);

        assertEquals(0, chatHistoryService.getHistory(testUserId).size());
    }

    @Test
    void testEmptyAndNullSafety() {
        assertNull(chatHistoryService.saveMessage(null, "user", "Hello", "English"));
        assertNull(chatHistoryService.saveMessage(testUserId, "user", "", "English"));
        assertNull(chatHistoryService.saveMessage(testUserId, "user", "   ", "English"));
        assertTrue(chatHistoryService.getHistory(null).isEmpty());
    }
}
