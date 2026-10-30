package com.ayurveda.bot;

import com.ayurveda.bot.controller.AuthController;
import com.ayurveda.bot.service.ChatHistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ChatAndAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Test
    void testUnauthenticatedAccessRedirectsOrReturns401() throws Exception {
        mockMvc.perform(get("/chat"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/api/chat/history"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/chat/history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAuthenticatedUserRedirectedFromLoginAndSignup() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthController.USER_ID, 1234L);
        session.setAttribute(AuthController.USER_NAME, "Test User");
        session.setAttribute(AuthController.USER_EMAIL, "test@example.com");

        // Visiting login while logged in should redirect to chat
        mockMvc.perform(get("/login").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/chat"));

        // Visiting signup while logged in should redirect to chat
        mockMvc.perform(get("/signup").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/chat"));
    }

    @Test
    void testAuthenticatedChatHistoryFlow() throws Exception {
        Long userId = 7777L;
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthController.USER_ID, userId);
        session.setAttribute(AuthController.USER_NAME, "Ayurveda Fan");

        // Seed some history
        chatHistoryService.saveMessage(userId, "user", "What is Triphala?", "English");
        chatHistoryService.saveMessage(userId, "bot", "Triphala is an Ayurvedic herbal rasayana.", "English");

        // Retrieve via API
        mockMvc.perform(get("/api/chat/history").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sender").value("user"))
                .andExpect(jsonPath("$[0].content").value("What is Triphala?"))
                .andExpect(jsonPath("$[1].sender").value("bot"));

        // Clear history
        mockMvc.perform(delete("/api/chat/history").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Check history is now empty
        mockMvc.perform(get("/api/chat/history").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
