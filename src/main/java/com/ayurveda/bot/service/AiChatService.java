package com.ayurveda.bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

@Service
public class AiChatService {

    @Value("${ai.provider:gemini}")
    private String aiProvider;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-flash-latest}")
    private String geminiModel;

    @Value("${grok.api.key:}")
    private String grokApiKey;

    @Value("${grok.model:grok-2-latest}")
    private String grokModel;

    @Value("${grok.api.url:https://api.x.ai/v1/chat/completions}")
    private String grokApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ============================================================
    // MAIN CHAT METHOD
    // ============================================================

    public void streamChat(String language, String question, Consumer<String> onChunk) {
        language = normalizeLanguage(language);

        if (question == null || question.isBlank()) {
            onChunk.accept(getOutOfDomainMessage(language));
            return;
        }

        question = question.trim();

        // Ayurvedic and health domain restriction
        if (!isAyurvedaOrHealthRelated(question)) {
            onChunk.accept(getOutOfDomainMessage(language));
            return;
        }

        String systemPrompt = buildSystemPrompt(language);

        // Select AI provider
        boolean useGrok = "grok".equalsIgnoreCase(aiProvider);
        if (useGrok) {
            streamGrokChat(systemPrompt, question, onChunk);
        } else {
            streamGeminiChat(systemPrompt, question, onChunk);
        }
    }

    // ============================================================
    // GOOGLE GEMINI STREAMING (FREE TIER)
    // ============================================================

    private void streamGeminiChat(String systemPrompt, String question, Consumer<String> onChunk) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            onChunk.accept("\n\nGoogle Gemini API key is missing. Please set GEMINI_API_KEY in application.properties or .env.");
            return;
        }

        String model = (geminiModel != null && !geminiModel.isBlank()) ? geminiModel : "gemini-flash-latest";
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":streamGenerateContent?alt=sse&key=" + geminiApiKey;

        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", systemPrompt))
        );

        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", question))
        );

        Map<String, Object> generationConfig = Map.of(
                "temperature", 0.2,
                "maxOutputTokens", 1200
        );

        Map<String, Object> request = Map.of(
                "system_instruction", systemInstruction,
                "contents", List.of(userContent),
                "generationConfig", generationConfig
        );

        try {
            restTemplate.execute(
                    url,
                    HttpMethod.POST,
                    requestCallback -> {
                        requestCallback.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                        requestCallback.getHeaders().setAccept(List.of(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON));
                        objectMapper.writeValue(requestCallback.getBody(), request);
                    },
                    response -> {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.isBlank()) {
                                    continue;
                                }
                                if (line.startsWith("data: ")) {
                                    String jsonStr = line.substring(6).trim();
                                    if (jsonStr.isEmpty() || "[DONE]".equals(jsonStr)) {
                                        continue;
                                    }

                                    JsonNode root = objectMapper.readTree(jsonStr);

                                    // Check for error in payload
                                    if (root.has("error")) {
                                        String errorMsg = root.path("error").path("message").asText();
                                        onChunk.accept("\n[Gemini Error: " + errorMsg + "]");
                                        continue;
                                    }

                                    JsonNode candidates = root.path("candidates");
                                    if (candidates.isArray() && !candidates.isEmpty()) {
                                        JsonNode parts = candidates.get(0).path("content").path("parts");
                                        if (parts.isArray()) {
                                            for (JsonNode part : parts) {
                                                if (part.has("text")) {
                                                    String text = part.path("text").asText();
                                                    if (!text.isEmpty()) {
                                                        onChunk.accept(text);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        return null;
                    }
            );
        } catch (HttpStatusCodeException e) {
            String errorDetail = extractErrorMessage(e.getResponseBodyAsString());
            onChunk.accept("\n\nUnable to generate answer from Gemini API (" + e.getStatusCode() + "):\n" + errorDetail);
        } catch (Exception e) {
            onChunk.accept("\n\nUnable to connect to Google Gemini API. Please check your network connection and API key.");
        }
    }

    // ============================================================
    // XAI GROK STREAMING
    // ============================================================

    private void streamGrokChat(String systemPrompt, String question, Consumer<String> onChunk) {
        if (grokApiKey == null || grokApiKey.isBlank()) {
            onChunk.accept("\n\nGrok API key is missing. Please set GROK_API_KEY in application.properties or .env.");
            return;
        }

        String model = (grokModel != null && !grokModel.isBlank()) ? grokModel : "grok-2-latest";
        String url = (grokApiUrl != null && !grokApiUrl.isBlank()) ? grokApiUrl : "https://api.x.ai/v1/chat/completions";

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", question)
        );

        Map<String, Object> request = Map.of(
                "model", model,
                "messages", messages,
                "stream", true,
                "temperature", 0.2
        );

        try {
            restTemplate.execute(
                    url,
                    HttpMethod.POST,
                    requestCallback -> {
                        requestCallback.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                        requestCallback.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + grokApiKey.trim());
                        requestCallback.getHeaders().setAccept(List.of(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON));
                        objectMapper.writeValue(requestCallback.getBody(), request);
                    },
                    response -> {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.isBlank()) {
                                    continue;
                                }
                                if (line.startsWith("data: ")) {
                                    String jsonStr = line.substring(6).trim();
                                    if ("[DONE]".equals(jsonStr)) {
                                        break;
                                    }
                                    if (jsonStr.isEmpty()) {
                                        continue;
                                    }

                                    JsonNode root = objectMapper.readTree(jsonStr);
                                    JsonNode choices = root.path("choices");
                                    if (choices.isArray() && !choices.isEmpty()) {
                                        JsonNode delta = choices.get(0).path("delta");
                                        if (delta.has("content")) {
                                            String content = delta.path("content").asText();
                                            if (!content.isEmpty()) {
                                                onChunk.accept(content);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        return null;
                    }
            );
        } catch (HttpStatusCodeException e) {
            String errorDetail = extractErrorMessage(e.getResponseBodyAsString());
            onChunk.accept("\n\nUnable to generate answer from Grok API (" + e.getStatusCode() + "):\n" + errorDetail);
        } catch (Exception e) {
            onChunk.accept("\n\nUnable to connect to xAI Grok API. Please check your network connection and Grok API key.");
        }
    }

    private String extractErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return "No additional error details received.";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.has("error")) {
                JsonNode errNode = root.get("error");
                if (errNode.isTextual()) {
                    return errNode.asText();
                } else if (errNode.has("message")) {
                    return errNode.path("message").asText();
                }
            }
            if (root.has("message")) {
                return root.path("message").asText();
            }
        } catch (Exception ignored) {
        }
        return body;
    }

    // ============================================================
    // LANGUAGE NORMALIZATION
    // ============================================================

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "English";
        }
        String value = language.trim().toLowerCase(Locale.ROOT);
        if (value.equals("hindi") || value.equals("हिंदी") || value.equals("hin")) {
            return "Hindi";
        }
        if (value.equals("marathi") || value.equals("मराठी") || value.equals("mar")) {
            return "Marathi";
        }
        return "English";
    }

    // ============================================================
    // HEALTH / AYURVEDA FILTER
    // ============================================================

    private boolean isAyurvedaOrHealthRelated(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }

        String q = question.toLowerCase(Locale.ROOT).trim();

        String[] ayurvedaKeywords = {
                "ayurveda", "ayurvedic", "ayurved", "वेद", "आयुर्वेद", "आयुर्वेदिक",
                "vata", "vata dosha", "वात", "pitta", "pitta dosha", "पित्त",
                "kapha", "kapha dosha", "कफ", "dosha", "दोष", "prakriti", "prakruti",
                "प्रकृति", "प्रकृती", "vikriti", "vikruti", "agni", "अग्नि", "अग्नी",
                "ama", "आम", "dinacharya", "दिनचर्या", "ritucharya", "ऋतुचर्या",
                "panchakarma", "पंचकर्म", "rasayana", "रसायन", "churna", "चूर्ण",
                "kwath", "क्वाथ", "kadha", "काढा", "arishta", "अरिष्ट", "asava", "आसव",
                "bhasma", "भस्म", "ghrita", "घृत", "taila", "तैल"
        };

        if (containsAny(q, ayurvedaKeywords)) {
            return true;
        }

        String[] healthKeywords = {
                "health", "healthy", "wellness", "fitness", "lifestyle", "body",
                "disease", "illness", "symptom", "symptoms", "problem", "sick",
                "आरोग्य", "निरोगी", "आरोग्यदायी", "स्वास्थ्य", "स्वस्थ", "बीमारी", "रोग", "लक्षण",
                "fever", "fevr", "feaver", "fevar", "fevre", "feverr", "temperature", "बुखार", "ताप",
                "cold", "clod", "coldd", "cough", "cogh", "coughh", "cof",
                "sore throat", "throat pain", "runny nose", "blocked nose",
                "सर्दी", "जुकाम", "खांसी", "खोकला", "घसा", "घसा दुखणे",
                "headache", "hedache", "headche", "hedche", "migraine", "सिरदर्द", "डोकेदुखी",
                "pain", "stomach pain", "body pain", "back pain", "joint pain", "muscle pain", "abdominal pain",
                "वेदना", "अंगदुखी", "पोटदुखी", "कंबरदुखी", "सांधेदुखी",
                "weakness", "wekness", "weaknes", "weknes", "fatigue", "tired", "tiredness", "dizziness",
                "कमजोरी", "कमजोर", "थकान", "चक्कर", "अशक्तपणा", "थकवा",
                "vomiting", "vomitting", "vomitng", "nausea", "nausia",
                "diarrhea", "diarhea", "diarrhoea", "diarhhea", "constipation", "constiption",
                "gas", "bloating", "acidity", "acidty", "indigestion", "heartburn",
                "उल्टी", "मळमळ", "दस्त", "जुलाब", "कब्ज", "बद्धकोष्ठता", "गॅस", "आम्लपित्त", "अपचन",
                "itching", "itch", "rash", "allergy", "infection", "skin problem",
                "खुजली", "खाज", "पुरळ", "अॅलर्जी",
                "hair fall", "hairfall", "hair loss", "dandruff", "केस गळणे", "कोंडा",
                "stress", "stres", "anxiety", "anxity", "sleep", "insomnia", "sleep problem",
                "तनाव", "चिंता", "नींद", "निद्रानाश", "तणाव",
                "diet", "food", "nutrition", "healthy food", "healthy diet", "आहार", "पोषण", "खाना", "भोजन",
                "exercise", "workout", "walking", "running", "व्यायाम", "चालणे", "धावणे",
                "yoga", "pranayama", "meditation", "योग", "प्राणायाम", "ध्यान"
        };

        if (containsAny(q, healthKeywords)) {
            return true;
        }

        String[] naturalKeywords = {
                "herb", "herbs", "herbal", "remedy", "remedies",
                "natural remedy", "natural remedies", "home remedy", "home remedies",
                "natural treatment", "medicine", "medicines", "ayurvedic medicine",
                "ashwagandha", "ashwaganda", "tulsi", "tulasi", "neem", "turmeric", "haldi",
                "ginger", "aloe", "aloe vera", "brahmi", "amla", "shatavari", "giloy",
                "moringa", "triphala", "arjuna",
                "जड़ी-बूटी", "जड़ी बूटी", "औषधीय वनस्पति", "औषधी वनस्पती",
                "नैसर्गिक उपाय", "नैसर्गिक उपचार", "घरगुती उपाय"
        };

        if (containsAny(q, naturalKeywords)) {
            return true;
        }

        return fuzzyHealthMatch(q);
    }

    // ============================================================
    // FUZZY HEALTH MATCH
    // ============================================================

    private boolean fuzzyHealthMatch(String question) {
        String cleaned = question.replaceAll("[^a-zA-Z0-9\\s]", " ").toLowerCase(Locale.ROOT);
        String[] words = cleaned.split("\\s+");

        String[] fuzzyWords = {
                "health", "healthy", "wellness", "fitness", "lifestyle",
                "disease", "illness", "symptom", "symptoms", "fever", "temperature",
                "cold", "cough", "throat", "headache", "migraine", "pain", "stomach",
                "abdominal", "back", "joint", "muscle", "weakness", "fatigue", "tired",
                "dizziness", "vomiting", "nausea", "diarrhea", "diarrhoea", "constipation",
                "acidity", "indigestion", "heartburn", "bloating", "gas", "itching", "rash",
                "allergy", "infection", "stress", "anxiety", "sleep", "insomnia", "diet",
                "food", "nutrition", "exercise", "workout", "yoga", "pranayama", "meditation",
                "ayurveda", "ayurvedic", "vata", "pitta", "kapha", "dosha", "prakriti",
                "prakruti", "vikriti", "vikruti", "agni", "dinacharya", "ritucharya",
                "panchakarma", "rasayana", "medicine", "medicines", "remedy", "remedies",
                "treatment", "herb", "herbs", "herbal", "ashwagandha", "tulsi", "tulasi",
                "neem", "turmeric", "haldi", "ginger", "aloe", "brahmi", "amla", "shatavari",
                "giloy", "moringa", "triphala", "arjuna"
        };

        for (String word : words) {
            if (word.length() < 3) {
                continue;
            }
            for (String target : fuzzyWords) {
                int distance = levenshteinDistance(word, target);
                int allowedDistance = getAllowedDistance(target);
                if (distance <= allowedDistance) {
                    return true;
                }
            }
        }

        String[] phrases = {
                "sore throat", "throat pain", "stomach pain", "body pain", "back pain",
                "joint pain", "muscle pain", "hair fall", "natural remedy", "home remedy",
                "ayurvedic medicine", "healthy diet", "healthy food", "sleep problem"
        };

        for (int i = 0; i < words.length - 1; i++) {
            String combined = words[i] + " " + words[i + 1];
            for (String phrase : phrases) {
                int distance = levenshteinDistance(combined, phrase);
                if (distance <= 3) {
                    return true;
                }
            }
        }

        return false;
    }

    private int getAllowedDistance(String target) {
        int length = target.length();
        if (length <= 4) {
            return 1;
        }
        if (length <= 7) {
            return 2;
        }
        return 3;
    }

    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    // ============================================================
    // SYSTEM PROMPT
    // ============================================================

    private String buildSystemPrompt(String language) {
        return """
                You are AyurvedaCare.

                You are an AI assistant focused on:
                - Ayurveda
                - General health
                - Medicinal herbs
                - Natural remedies
                - Ayurvedic diet
                - Nutrition
                - Yoga
                - Pranayama
                - Meditation
                - Sleep
                - Healthy lifestyle
                - Natural wellness

                ============================================================
                IMPORTANT
                ============================================================
                Understand the user's intended meaning even when they make spelling mistakes.
                Example: "I have fevr" means "I have fever". Do not reject health questions because of spelling mistakes.

                ============================================================
                RESPONSE STRUCTURE (for symptom / illness questions)
                ============================================================
                Whenever the user describes a symptom or health complaint (e.g. fever, cold, headache, stomach pain, weakness), structure your answer using these sections, in this order, with short bullet points under each:

                1. Possible Causes
                   - 2-4 common, general causes.

                2. Ayurvedic Remedies
                   - 2-4 practical Ayurvedic or home remedies (herbs, kadha, diet adjustments, etc.).

                3. Pranayama / Yoga
                   - 1-3 relevant breathing exercises or yoga practices suitable for the condition.
                     Skip this section only if rest/bed rest is medically advised instead (e.g. very high fever).

                4. When to See a Doctor
                   - Brief note on warning signs that need professional medical attention.

                Use these exact section headers (translated into the response language), keep each bullet short, and do not pad with repeated information.

                For general questions that are NOT about a specific symptom (e.g. "what is Ayurveda", "benefits of yoga"), you do not need to use this structure — answer normally and directly.

                ============================================================
                RESPONSE RULES
                ============================================================
                Give a direct answer to the user's question.
                Do not unnecessarily repeat information.
                Do not invent words.
                Do not generate meaningless sentences.
                Do not make a diagnosis.
                Do not promise a cure.
                Do not tell users to stop prescribed medicines.
                Do not provide dangerous medical instructions.
                For serious or worsening symptoms, recommend seeing a qualified healthcare professional.

                ============================================================
                LANGUAGE
                ============================================================
                Respond in: """ + language + """

                If the language is Hindi:
                Use natural, grammatically correct Hindi in Devanagari script.
                Use standard Hindi vocabulary.
                Do not translate English word-by-word.

                If the language is Marathi:
                Use natural, grammatically correct Marathi in Devanagari script.
                Use standard Marathi vocabulary.
                Do not translate English word-by-word.

                If the language is English:
                Use clear, natural English.

                ============================================================
                MEDICAL SAFETY
                ============================================================
                You are an educational assistant and not a doctor.
                For serious symptoms, persistent symptoms, very high fever, difficulty breathing, confusion, severe dehydration, seizures, severe headache, stiff neck, persistent vomiting, or other emergency symptoms, recommend prompt medical attention.

                ============================================================
                FINAL RULE
                ============================================================
                Stay focused on health, Ayurveda, natural wellness, herbs, diet and healthy lifestyle.
                If a user asks about a completely unrelated topic, do not answer that topic.
                """;
    }

    // ============================================================
    // OUT OF DOMAIN MESSAGE
    // ============================================================

    private String getOutOfDomainMessage(String language) {
        if (language.equalsIgnoreCase("Hindi")) {
            return """
                    मैं AyurvedaCare हूँ — आयुर्वेद, स्वास्थ्य और प्राकृतिक जीवनशैली पर केंद्रित AI सहायक।

                    मैं केवल स्वास्थ्य, आयुर्वेद, औषधीय जड़ी-बूटियों, प्राकृतिक उपाय, आहार, योग, ध्यान, नींद और स्वस्थ जीवनशैली से संबंधित प्रश्नों में मदद कर सकता हूँ।

                    कृपया स्वास्थ्य या आयुर्वेद से संबंधित प्रश्न पूछें।
                    """;
        }

        if (language.equalsIgnoreCase("Marathi")) {
            return """
                    मी AyurvedaCare आहे — आयुर्वेद, आरोग्य आणि नैसर्गिक जीवनशैलीवर केंद्रित AI सहाय्यक.

                    मी फक्त आरोग्य, आयुर्वेद, औषधी वनस्पती, नैसर्गिक उपाय, आहार, योग, ध्यान, झोप आणि निरोगी जीवनशैलीशी संबंधित प्रश्नांमध्ये मदत करू शकतो.

                    कृपया आरोग्य किंवा आयुर्वेदाशी संबंधित प्रश्न विचारा.
                    """;
        }

        return """
                I am AyurvedaCare — an AI assistant focused on Ayurveda, health and natural lifestyle.

                I can help with:
                • Ayurveda
                • Ayurvedic medicines
                • Medicinal herbs
                • General health questions
                • Natural remedies
                • Ayurvedic diet
                • Yoga and Pranayama
                • Meditation
                • Sleep
                • Natural wellness
                • Healthy lifestyle

                Please ask a health, Ayurveda, herb or natural lifestyle related question.
                """;
    }
}
