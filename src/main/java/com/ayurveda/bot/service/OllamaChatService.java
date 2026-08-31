package com.ayurveda.bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class OllamaChatService {

    @Value("${ollama.api.url}")
    private String apiUrl;

    @Value("${ollama.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();


    // ============================================================
    // MAIN CHAT METHOD
    // ============================================================

    public void streamChat(
            String language,
            String question,
            Consumer<String> onChunk) {

        // --------------------------------------------------------
        // Normalize language
        // --------------------------------------------------------

        language = normalizeLanguage(language);

        // --------------------------------------------------------
        // Validate question
        // --------------------------------------------------------

        if (question == null || question.isBlank()) {

            onChunk.accept(
                    getOutOfDomainMessage(language)
            );

            return;
        }

        question = question.trim();


        // --------------------------------------------------------
        // HEALTH / AYURVEDA RESTRICTION
        // --------------------------------------------------------

        /*
         * IMPORTANT:
         *
         * The question is checked BEFORE Ollama is called.
         *
         * If it is not related to health, Ayurveda, herbs,
         * natural lifestyle, diet, yoga, sleep, etc.,
         * Ollama will NOT receive the question.
         */

        if (!isAyurvedaOrHealthRelated(question)) {

            onChunk.accept(
                    getOutOfDomainMessage(language)
            );

            return;
        }


        // --------------------------------------------------------
        // Build system prompt
        // --------------------------------------------------------

        String systemPrompt =
                buildSystemPrompt(language);


        try {

            // ----------------------------------------------------
            // Create messages
            // ----------------------------------------------------

            List<Map<String, String>> messages =
                    new ArrayList<>();


            messages.add(
                    Map.of(
                            "role",
                            "system",

                            "content",
                            systemPrompt
                    )
            );


            /*
             * IMPORTANT:
             *
             * Send the ORIGINAL question to Ollama.
             *
             * Example:
             *
             * User:
             *     i have fevr
             *
             * Filter:
             *     fevr -> fever
             *
             * Ollama still receives:
             *     i have fevr
             */

            messages.add(
                    Map.of(
                            "role",
                            "user",

                            "content",
                            question
                    )
            );


            // ----------------------------------------------------
            // Ollama request
            // ----------------------------------------------------

            Map<String, Object> request =
                    Map.of(

                            "model",
                            model,

                            "messages",
                            messages,

                            "stream",
                            true,

                            "options",
                            Map.of(

                                    /*
                                     * Low temperature gives
                                     * more consistent answers.
                                     */
                                    "temperature",
                                    0.2,

                                    /*
                                     * Maximum generated tokens.
                                     *
                                     * Increased from 400 to 700
                                     * so structured 4-section
                                     * answers (Causes / Remedies /
                                     * Pranayama / When to see a
                                     * doctor) are not truncated.
                                     */
                                    "num_predict",
                                    700,

                                    /*
                                     * Reduces repeated phrases.
                                     */
                                    "repeat_penalty",
                                    1.1,

                                    /*
                                     * Keeps answers focused.
                                     */
                                    "top_p",
                                    0.9
                            )
                    );


            // ----------------------------------------------------
            // Execute Ollama request
            // ----------------------------------------------------

            restTemplate.execute(

                    apiUrl,

                    org.springframework.http.HttpMethod.POST,

                    requestCallback -> {

                        requestCallback
                                .getHeaders()
                                .setContentType(
                                        MediaType.APPLICATION_JSON
                                );

                        requestCallback
                                .getHeaders()
                                .setAccept(
                                        List.of(
                                                MediaType.APPLICATION_NDJSON,
                                                MediaType.APPLICATION_JSON
                                        )
                                );


                        objectMapper.writeValue(
                                requestCallback.getBody(),
                                request
                        );
                    },


                    response -> {

                        try {

                            BufferedReader reader =
                                    new BufferedReader(
                                            new InputStreamReader(
                                                    response.getBody(),
                                                    StandardCharsets.UTF_8
                                            )
                                    );


                            String line;


                            while (
                                    (line =
                                            reader.readLine())
                                            != null
                            ) {

                                if (line.isBlank()) {
                                    continue;
                                }


                                JsonNode json =
                                        objectMapper.readTree(line);


                                // --------------------------------
                                // Get generated content
                                // --------------------------------

                                JsonNode content =
                                        json
                                                .path("message")
                                                .path("content");


                                if (
                                        !content.isMissingNode()
                                                &&
                                        !content.isNull()
                                ) {

                                    String chunk =
                                            content.asText();


                                    if (!chunk.isEmpty()) {

                                        onChunk.accept(chunk);
                                    }
                                }


                                // --------------------------------
                                // Check if stream finished
                                // --------------------------------

                                if (
                                        json
                                                .path("done")
                                                .asBoolean(false)
                                ) {

                                    break;
                                }
                            }


                        } catch (Exception e) {

                            throw new RuntimeException(
                                    "Error reading Ollama response",
                                    e
                            );
                        }


                        return null;
                    }
            );


        } catch (Exception e) {

            e.printStackTrace();

            onChunk.accept(
                    "\n\nUnable to connect to Ollama.\n"
                            + "Please make sure Ollama is running "
                            + "and the configured model is installed."
            );
        }
    }


    // ============================================================
    // LANGUAGE NORMALIZATION
    // ============================================================

    private String normalizeLanguage(
            String language) {

        if (language == null ||
                language.isBlank()) {

            return "English";
        }


        String value =
                language
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );


        // Hindi

        if (
                value.equals("hindi")
                        ||
                value.equals("हिंदी")
                        ||
                value.equals("hin")
        ) {

            return "Hindi";
        }


        // Marathi

        if (
                value.equals("marathi")
                        ||
                value.equals("मराठी")
                        ||
                value.equals("mar")
        ) {

            return "Marathi";
        }


        // English

        if (
                value.equals("english")
                        ||
                value.equals("eng")
        ) {

            return "English";
        }


        return "English";
    }


    // ============================================================
    // HEALTH / AYURVEDA FILTER
    // ============================================================

    private boolean isAyurvedaOrHealthRelated(
            String question) {

        if (question == null ||
                question.isBlank()) {

            return false;
        }


        String q =
                question
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .trim();


        // ========================================================
        // AYURVEDA
        // ========================================================

        String[] ayurvedaKeywords = {

                "ayurveda",
                "ayurvedic",
                "ayurved",

                "वेद",
                "आयुर्वेद",
                "आयुर्वेदिक",

                "vata",
                "vata dosha",
                "वात",

                "pitta",
                "pitta dosha",
                "पित्त",

                "kapha",
                "kapha dosha",
                "कफ",

                "dosha",
                "दोष",

                "prakriti",
                "prakruti",
                "प्रकृति",
                "प्रकृती",

                "vikriti",
                "vikruti",

                "agni",
                "अग्नि",
                "अग्नी",

                "ama",
                "आम",

                "dinacharya",
                "दिनचर्या",

                "ritucharya",
                "ऋतुचर्या",

                "panchakarma",
                "पंचकर्म",

                "rasayana",
                "रसायन",

                "churna",
                "चूर्ण",

                "kwath",
                "क्वाथ",

                "kadha",
                "काढा",

                "arishta",
                "अरिष्ट",

                "asava",
                "आसव",

                "bhasma",
                "भस्म",

                "ghrita",
                "घृत",

                "taila",
                "तैल"
        };


        if (
                containsAny(
                        q,
                        ayurvedaKeywords
                )
        ) {

            return true;
        }


        // ========================================================
        // HEALTH
        // ========================================================

        String[] healthKeywords = {

                // -----------------------------------------------
                // General health
                // -----------------------------------------------

                "health",
                "healthy",
                "wellness",
                "fitness",
                "lifestyle",
                "body",

                "disease",
                "illness",
                "symptom",
                "symptoms",
                "problem",
                "sick",

                "आरोग्य",
                "निरोगी",
                "आरोग्यदायी",
                "स्वास्थ्य",
                "स्वस्थ",
                "बीमारी",
                "रोग",
                "लक्षण",


                // -----------------------------------------------
                // Fever
                // -----------------------------------------------

                "fever",
                "fevr",
                "feaver",
                "fevar",
                "fevre",
                "feverr",
                "temperature",

                "बुखार",
                "ताप",


                // -----------------------------------------------
                // Cold / cough
                // -----------------------------------------------

                "cold",
                "clod",
                "coldd",

                "cough",
                "cogh",
                "coughh",
                "cof",

                "sore throat",
                "throat pain",
                "runny nose",
                "blocked nose",

                "सर्दी",
                "जुकाम",
                "खांसी",
                "खोकला",
                "घसा",
                "घसा दुखणे",


                // -----------------------------------------------
                // Headache
                // -----------------------------------------------

                "headache",
                "hedache",
                "headche",
                "hedche",
                "migraine",

                "सिरदर्द",
                "डोकेदुखी",


                // -----------------------------------------------
                // Pain
                // -----------------------------------------------

                "pain",
                "stomach pain",
                "body pain",
                "back pain",
                "joint pain",
                "muscle pain",
                "abdominal pain",

                "वेदना",
                "अंगदुखी",
                "पोटदुखी",
                "कंबरदुखी",
                "सांधेदुखी",


                // -----------------------------------------------
                // Weakness / fatigue
                // -----------------------------------------------

                "weakness",
                "wekness",
                "weaknes",
                "weknes",
                "fatigue",
                "tired",
                "tiredness",
                "dizziness",

                "कमजोरी",
                "कमजोर",
                "थकान",
                "चक्कर",
                "अशक्तपणा",
                "थकवा",


                // -----------------------------------------------
                // Digestion
                // -----------------------------------------------

                "vomiting",
                "vomitting",
                "vomitng",

                "nausea",
                "nausia",

                "diarrhea",
                "diarhea",
                "diarrhoea",
                "diarhhea",

                "constipation",
                "constiption",

                "gas",
                "bloating",
                "acidity",
                "acidty",
                "indigestion",
                "heartburn",

                "उल्टी",
                "मळमळ",
                "दस्त",
                "जुलाब",
                "कब्ज",
                "बद्धकोष्ठता",
                "गॅस",
                "आम्लपित्त",
                "अपचन",


                // -----------------------------------------------
                // Skin
                // -----------------------------------------------

                "itching",
                "itch",
                "rash",
                "allergy",
                "infection",
                "skin problem",

                "खुजली",
                "खाज",
                "पुरळ",
                "अॅलर्जी",


                // -----------------------------------------------
                // Hair
                // -----------------------------------------------

                "hair fall",
                "hairfall",
                "hair loss",
                "dandruff",

                "केस गळणे",
                "कोंडा",


                // -----------------------------------------------
                // Mental health / sleep
                // -----------------------------------------------

                "stress",
                "stres",
                "anxiety",
                "anxity",
                "sleep",
                "insomnia",
                "sleep problem",

                "तनाव",
                "चिंता",
                "नींद",
                "निद्रानाश",
                "तणाव",


                // -----------------------------------------------
                // Diet / food
                // -----------------------------------------------

                "diet",
                "food",
                "nutrition",
                "healthy food",
                "healthy diet",

                "आहार",
                "पोषण",
                "खाना",
                "भोजन",


                // -----------------------------------------------
                // Exercise
                // -----------------------------------------------

                "exercise",
                "workout",
                "walking",
                "running",

                "व्यायाम",
                "चालणे",
                "धावणे",


                // -----------------------------------------------
                // Yoga
                // -----------------------------------------------

                "yoga",
                "pranayama",
                "meditation",

                "योग",
                "प्राणायाम",
                "ध्यान"
        };


        if (
                containsAny(
                        q,
                        healthKeywords
                )
        ) {

            return true;
        }


        // ========================================================
        // HERBS / NATURAL REMEDIES
        // ========================================================

        String[] naturalKeywords = {

                "herb",
                "herbs",
                "herbal",

                "remedy",
                "remedies",

                "natural remedy",
                "natural remedies",

                "home remedy",
                "home remedies",

                "natural treatment",

                "medicine",
                "medicines",
                "ayurvedic medicine",

                "ashwagandha",
                "ashwaganda",

                "tulsi",
                "tulasi",

                "neem",

                "turmeric",
                "haldi",

                "ginger",

                "aloe",
                "aloe vera",

                "brahmi",

                "amla",

                "shatavari",

                "giloy",

                "moringa",

                "triphala",

                "arjuna",

                "जड़ी-बूटी",
                "जड़ी बूटी",
                "औषधीय वनस्पति",
                "औषधी वनस्पती",
                "नैसर्गिक उपाय",
                "नैसर्गिक उपचार",
                "घरगुती उपाय"
        };


        if (
                containsAny(
                        q,
                        naturalKeywords
                )
        ) {

            return true;
        }


        // ========================================================
        // FUZZY MATCHING
        // ========================================================

        return fuzzyHealthMatch(q);
    }


    // ============================================================
    // FUZZY HEALTH MATCH
    // ============================================================

    private boolean fuzzyHealthMatch(
            String question) {

        /*
         * Remove punctuation.
         */

        String cleaned =
                question
                        .replaceAll(
                                "[^a-zA-Z0-9\\s]",
                                " "
                        )
                        .toLowerCase(
                                Locale.ROOT
                        );


        String[] words =
                cleaned.split("\\s+");


        // ========================================================
        // FUZZY WORDS
        // ========================================================

        String[] fuzzyWords = {

                "health",
                "healthy",
                "wellness",
                "fitness",
                "lifestyle",

                "disease",
                "illness",
                "symptom",
                "symptoms",

                "fever",
                "temperature",

                "cold",
                "cough",
                "throat",

                "headache",
                "migraine",

                "pain",
                "stomach",
                "abdominal",
                "back",
                "joint",
                "muscle",

                "weakness",
                "fatigue",
                "tired",
                "dizziness",

                "vomiting",
                "nausea",

                "diarrhea",
                "diarrhoea",

                "constipation",
                "acidity",
                "indigestion",
                "heartburn",
                "bloating",
                "gas",

                "itching",
                "rash",
                "allergy",
                "infection",

                "stress",
                "anxiety",
                "sleep",
                "insomnia",

                "diet",
                "food",
                "nutrition",

                "exercise",
                "workout",

                "yoga",
                "pranayama",
                "meditation",

                "ayurveda",
                "ayurvedic",

                "vata",
                "pitta",
                "kapha",
                "dosha",

                "prakriti",
                "prakruti",

                "vikriti",
                "vikruti",

                "agni",

                "dinacharya",
                "ritucharya",

                "panchakarma",
                "rasayana",

                "medicine",
                "medicines",

                "remedy",
                "remedies",

                "treatment",

                "herb",
                "herbs",
                "herbal",

                "ashwagandha",
                "tulsi",
                "tulasi",
                "neem",
                "turmeric",
                "haldi",
                "ginger",
                "aloe",
                "brahmi",
                "amla",
                "shatavari",
                "giloy",
                "moringa",
                "triphala",
                "arjuna"
        };


        // ========================================================
        // CHECK WORDS
        // ========================================================

        for (String word : words) {

            if (word.length() < 3) {
                continue;
            }


            for (String target : fuzzyWords) {

                int distance =
                        levenshteinDistance(
                                word,
                                target
                        );


                int allowedDistance =
                        getAllowedDistance(
                                target
                        );


                if (distance <= allowedDistance) {

                    return true;
                }
            }
        }


        // ========================================================
        // CHECK COMMON PHRASES
        // ========================================================

        String[] phrases = {

                "sore throat",
                "throat pain",

                "stomach pain",
                "body pain",
                "back pain",

                "joint pain",
                "muscle pain",

                "hair fall",

                "natural remedy",
                "home remedy",

                "ayurvedic medicine",

                "healthy diet",
                "healthy food",

                "sleep problem"
        };


        for (int i = 0;
             i < words.length - 1;
             i++) {

            String combined =
                    words[i]
                            + " "
                            + words[i + 1];


            for (String phrase : phrases) {

                int distance =
                        levenshteinDistance(
                                combined,
                                phrase
                        );


                if (distance <= 3) {

                    return true;
                }
            }
        }


        return false;
    }


    // ============================================================
    // FUZZY DISTANCE
    // ============================================================

    private int getAllowedDistance(
            String target) {

        int length =
                target.length();


        if (length <= 4) {
            return 1;
        }


        if (length <= 7) {
            return 2;
        }


        return 3;
    }


    // ============================================================
    // LEVENSHTEIN DISTANCE
    // ============================================================

    private int levenshteinDistance(
            String a,
            String b) {

        int[][] dp =
                new int[
                        a.length() + 1
                        ][
                        b.length() + 1
                        ];


        // First column

        for (int i = 0;
             i <= a.length();
             i++) {

            dp[i][0] = i;
        }


        // First row

        for (int j = 0;
             j <= b.length();
             j++) {

            dp[0][j] = j;
        }


        // Calculate

        for (int i = 1;
             i <= a.length();
             i++) {

            for (int j = 1;
                 j <= b.length();
                 j++) {

                int cost =
                        a.charAt(i - 1)
                                ==
                        b.charAt(j - 1)
                                ? 0
                                : 1;


                dp[i][j] =
                        Math.min(

                                Math.min(

                                        dp[i - 1][j] + 1,

                                        dp[i][j - 1] + 1
                                ),

                                dp[i - 1][j - 1]
                                        + cost
                        );
            }
        }


        return dp[
                a.length()
                ][
                b.length()
                ];
    }


    // ============================================================
    // CONTAINS ANY KEYWORD
    // ============================================================

    private boolean containsAny(
            String text,
            String... keywords) {

        for (String keyword : keywords) {

            if (
                    text.contains(
                            keyword.toLowerCase(
                                    Locale.ROOT
                            )
                    )
            ) {

                return true;
            }
        }


        return false;
    }


    // ============================================================
    // SYSTEM PROMPT
    // ============================================================

    private String buildSystemPrompt(
            String language) {

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

                Understand the user's intended meaning even when
                they make spelling mistakes.

                Example:

                "I have fevr"

                means:

                "I have fever"

                Do not reject health questions because of spelling
                mistakes.

                ============================================================
                RESPONSE STRUCTURE (for symptom / illness questions)
                ============================================================

                Whenever the user describes a symptom or health
                complaint (e.g. fever, cold, headache, stomach pain,
                weakness), structure your answer using these sections,
                in this order, with short bullet points under each:

                1. Possible Causes
                   - 2-4 common, general causes.

                2. Ayurvedic Remedies
                   - 2-4 practical Ayurvedic or home remedies
                     (herbs, kadha, diet adjustments, etc.).

                3. Pranayama / Yoga
                   - 1-3 relevant breathing exercises or yoga
                     practices suitable for the condition.
                     Skip this section only if rest/bed rest is
                     medically advised instead (e.g. very high fever).

                4. When to See a Doctor
                   - Brief note on warning signs that need
                     professional medical attention.

                Use these exact section headers (translated into the
                response language), keep each bullet short, and do not
                pad with repeated information.

                For general questions that are NOT about a specific
                symptom (e.g. "what is Ayurveda", "benefits of yoga"),
                you do not need to use this structure — answer normally
                and directly.

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

                For serious or worsening symptoms, recommend seeing
                a qualified healthcare professional.

                ============================================================
                LANGUAGE
                ============================================================

                Respond in:

                """
                + language
                + """

                If the language is Hindi:

                Use natural, grammatically correct Hindi.

                Use standard Hindi vocabulary.

                Use Devanagari script.

                Do not translate English word-by-word.

                Do not mix Hindi and Marathi unnecessarily.

                Do not invent Hindi words.

                Do not repeat phrases.

                If the language is Marathi:

                Use natural, grammatically correct Marathi.

                Use standard Marathi vocabulary.

                Use Devanagari script.

                Do not translate English word-by-word.

                Do not invent Marathi words.

                Do not repeat phrases.

                If the language is English:

                Use clear, natural English.

                ============================================================
                MEDICAL SAFETY
                ============================================================

                You are an educational assistant and not a doctor.

                For serious symptoms, persistent symptoms, very high
                fever, difficulty breathing, confusion, severe
                dehydration, seizures, severe headache, stiff neck,
                persistent vomiting, or other emergency symptoms,
                recommend prompt medical attention.

                ============================================================
                FINAL RULE
                ============================================================

                Stay focused on health, Ayurveda, natural wellness,
                herbs, diet and healthy lifestyle.

                If a user asks about a completely unrelated topic,
                do not answer that topic.
                """;
    }


    // ============================================================
    // OUT OF DOMAIN MESSAGE
    // ============================================================

    private String getOutOfDomainMessage(
            String language) {


        // ========================================================
        // HINDI
        // ========================================================

        if (
                language.equalsIgnoreCase(
                        "Hindi"
                )
        ) {

            return """
                    मैं AyurvedaCare हूँ — आयुर्वेद, स्वास्थ्य और
                    प्राकृतिक जीवनशैली पर केंद्रित AI सहायक।

                    मैं केवल स्वास्थ्य, आयुर्वेद, औषधीय जड़ी-बूटियों,
                    प्राकृतिक उपाय, आहार, योग, ध्यान, नींद और
                    स्वस्थ जीवनशैली से संबंधित प्रश्नों में मदद कर सकता हूँ।

                    कृपया स्वास्थ्य या आयुर्वेद से संबंधित प्रश्न पूछें।
                    """;
        }


        // ========================================================
        // MARATHI
        // ========================================================

        if (
                language.equalsIgnoreCase(
                        "Marathi"
                )
        ) {

            return """
                    मी AyurvedaCare आहे — आयुर्वेद, आरोग्य आणि
                    नैसर्गिक जीवनशैलीवर केंद्रित AI सहाय्यक.

                    मी फक्त आरोग्य, आयुर्वेद, औषधी वनस्पती,
                    नैसर्गिक उपाय, आहार, योग, ध्यान, झोप आणि
                    निरोगी जीवनशैलीशी संबंधित प्रश्नांमध्ये
                    मदत करू शकतो.

                    कृपया आरोग्य किंवा आयुर्वेदाशी संबंधित
                    प्रश्न विचारा.
                    """;
        }


        // ========================================================
        // ENGLISH
        // ========================================================

        return """
                I am AyurvedaCare — an AI assistant focused on
                Ayurveda, health and natural lifestyle.

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

                Please ask a health, Ayurveda, herb or natural
                lifestyle related question.
                """;
    }
}