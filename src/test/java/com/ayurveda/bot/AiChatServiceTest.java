package com.ayurveda.bot;

import com.ayurveda.bot.service.AiChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiChatServiceTest {

    @Autowired
    private AiChatService aiChatService;

    @Test
    void testOutOfDomainFiltering() {
        List<String> chunks = new ArrayList<>();
        aiChatService.streamChat("English", "What is the capital of Australia?", chunks::add);

        String fullResponse = String.join("", chunks);
        assertTrue(fullResponse.contains("AyurvedaCare"), "Should identify itself as AyurvedaCare");
        assertTrue(fullResponse.contains("Please ask a health, Ayurveda"), "Should redirect to health/Ayurveda");
    }

    @Test
    void testAyurvedaDomainRecognitionAndResponse() {
        List<String> chunks = new ArrayList<>();
        aiChatService.streamChat("English", "What are the benefits of Tulsi in Ayurveda?", chunks::add);

        String fullResponse = String.join("", chunks);
        assertFalse(fullResponse.isBlank(), "Response should not be blank");
        assertTrue(
                fullResponse.toLowerCase().contains("tulsi") || fullResponse.toLowerCase().contains("ayurveda") || fullResponse.toLowerCase().contains("herb"),
                "Response should mention Tulsi or Ayurveda: " + fullResponse
        );
    }
}
