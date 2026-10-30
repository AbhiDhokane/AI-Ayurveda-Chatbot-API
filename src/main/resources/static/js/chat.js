document.addEventListener("DOMContentLoaded", function () {

    const messagesContainer = document.getElementById("chat-messages");
    const userInput = document.getElementById("user-input");
    const sendBtn = document.getElementById("send-btn");
    const languageSelect = document.getElementById("language");
    const emailPanel = document.getElementById("email-panel");
    const clearHistoryBtn = document.getElementById("clear-history-btn");

    function formatTime(date) {
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }

    // ========================================================
    // USER MESSAGE
    // ========================================================

    function addUserMessage(message, timestamp) {
        const welcome = document.getElementById("welcome-message");
        if (welcome) welcome.remove();

        const messageDiv = document.createElement("div");
        messageDiv.className = "message user";

        const content = document.createElement("div");
        content.className = "message-content";
        content.textContent = message;

        if (timestamp) {
            const timeSpan = document.createElement("span");
            timeSpan.className = "message-time";
            timeSpan.textContent = timestamp;
            content.appendChild(timeSpan);
        }

        messageDiv.appendChild(content);
        messagesContainer.appendChild(messageDiv);
        scrollToBottom();
        return content;
    }

    // ========================================================
    // BOT MESSAGE
    // ========================================================

    function createBotMessage() {
        const welcome = document.getElementById("welcome-message");
        if (welcome) welcome.remove();

        const messageDiv = document.createElement("div");
        messageDiv.className = "message bot";

        const content = document.createElement("div");
        content.className = "message-content";

        messageDiv.appendChild(content);
        messagesContainer.appendChild(messageDiv);
        scrollToBottom();
        return content;
    }

    function addBotMessage(message, timestamp) {
        const welcome = document.getElementById("welcome-message");
        if (welcome) welcome.remove();

        const messageDiv = document.createElement("div");
        messageDiv.className = "message bot";

        const content = document.createElement("div");
        content.className = "message-content";
        content.textContent = message;

        if (timestamp) {
            const timeSpan = document.createElement("span");
            timeSpan.className = "message-time";
            timeSpan.textContent = timestamp;
            content.appendChild(timeSpan);
        }

        messageDiv.appendChild(content);
        messagesContainer.appendChild(messageDiv);
        scrollToBottom();
        return content;
    }

    // ========================================================
    // TYPING INDICATOR
    // ========================================================

    function showTypingIndicator() {
        const div = document.createElement("div");
        div.id = "typing-indicator";
        div.className = "message bot";
        div.innerHTML = `
            <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
            </div>
        `;
        messagesContainer.appendChild(div);
        scrollToBottom();
        return div;
    }

    // ========================================================
    // SCROLL
    // ========================================================

    function scrollToBottom() {
        messagesContainer.scrollTo({
            top: messagesContainer.scrollHeight,
            behavior: "smooth"
        });
    }

    // ========================================================
    // TRUE STREAMING
    // ========================================================

    function streamMessage(language, message) {
        return new Promise(function (resolve, reject) {
            const botElement = createBotMessage();

            const url = "/api/chat/stream?language="
                + encodeURIComponent(language)
                + "&message="
                + encodeURIComponent(message);

            const eventSource = new EventSource(url);
            let finished = false;

            eventSource.onmessage = function (event) {
                if (finished) return;
                try {
                    const chunk = JSON.parse(event.data);
                    botElement.textContent += chunk;
                    scrollToBottom();
                } catch (error) {
                    console.error("Invalid SSE chunk:", error);
                }
            };

            eventSource.addEventListener("done", function () {
                if (finished) return;
                finished = true;
                eventSource.close();
                resolve(botElement);
            });

            eventSource.onerror = function () {
                if (finished) return;
                if (botElement.textContent.length > 0) {
                    finished = true;
                    eventSource.close();
                    resolve(botElement);
                    return;
                }
                finished = true;
                eventSource.close();
                reject(new Error("SSE connection failed."));
            };
        });
    }

    // ========================================================
    // EMAIL AI ANSWER
    // ========================================================

    function showEmailAction(question, answer, language) {
        if (!emailPanel) return;
        emailPanel.hidden = false;
        emailPanel.innerHTML = `
            <strong>📧 Want this information by email?</strong>
            <form class="email-form" id="treatment-email-form">
                <input type="email" id="treatment-email" placeholder="Email address" value="${window.currentUserEmail || ""}" required>
                <button class="btn btn-secondary" type="submit">Send Treatment Info</button>
            </form>
            <div class="email-status" id="email-status"></div>
        `;
        const emailForm = document.getElementById("treatment-email-form");
        if (emailForm) {
            emailForm.addEventListener("submit", async function(event) {
                event.preventDefault();
                const status = document.getElementById("email-status");
                const email = document.getElementById("treatment-email").value.trim();
                status.textContent = "Sending...";
                try {
                    const response = await fetch("/api/email/treatment", {
                        method: "POST",
                        headers: {"Content-Type":"application/json"},
                        body: JSON.stringify({email, question, answer, language})
                    });
                    const data = await response.json();
                    status.textContent = data.message || "Done.";
                } catch (error) {
                    status.textContent = "Unable to send email. Check your SMTP configuration.";
                }
            });
        }
    }

    // ========================================================
    // LOAD CHAT HISTORY
    // ========================================================

    async function loadChatHistory() {
        try {
            const response = await fetch("/api/chat/history");
            if (!response.ok) return;
            const history = await response.json();
            if (Array.isArray(history) && history.length > 0) {
                let lastUserMsg = "";
                let lastBotMsg = "";
                let lastLang = "English";

                history.forEach(item => {
                    if (item.sender === "user") {
                        addUserMessage(item.content, item.timestamp);
                        lastUserMsg = item.content;
                        if (item.language) lastLang = item.language;
                    } else {
                        addBotMessage(item.content, item.timestamp);
                        lastBotMsg = item.content;
                        if (item.language) lastLang = item.language;
                    }
                });

                if (lastBotMsg && lastUserMsg) {
                    showEmailAction(lastUserMsg, lastBotMsg, lastLang);
                }
            }
        } catch (err) {
            console.warn("Could not load chat history:", err);
        }
    }

    // ========================================================
    // CLEAR CHAT HISTORY
    // ========================================================

    if (clearHistoryBtn) {
        clearHistoryBtn.addEventListener("click", async function () {
            if (!confirm("Are you sure you want to clear your chat history?")) {
                return;
            }
            try {
                const response = await fetch("/api/chat/history", {
                    method: "DELETE"
                });
                if (response.ok) {
                    messagesContainer.innerHTML = `
                        <div class="message bot welcome-message" id="welcome-message">
                            <div class="message-content">
                                Namaste! 🙏 I am AyurvedaCare, your Ayurvedic wellness companion.<br><br>
                                I can help you with:<br>
                                • Information about Ayurvedic herbs & medicines<br>
                                • Natural remedies and lifestyle tips<br>
                                • Understanding your Dosha (constitution)<br>
                                • Daily routines (Dinacharya) for natural fitness<br>
                                • Preferring herbal approaches whenever possible<br><br>
                                <strong>Which language would you prefer me to answer in?</strong> (Select above or just start chatting)
                            </div>
                        </div>
                    `;
                    if (emailPanel) {
                        emailPanel.hidden = true;
                        emailPanel.innerHTML = "";
                    }
                } else {
                    alert("Unable to clear chat history.");
                }
            } catch (err) {
                console.error(err);
                alert("Error connecting to server to clear history.");
            }
        });
    }

    // ========================================================
    // SEND MESSAGE
    // ========================================================

    async function sendMessage() {
        const message = userInput.value.trim();
        if (!message) return;

        const language = languageSelect.value || "English";

        sendBtn.disabled = true;
        userInput.disabled = true;

        addUserMessage(message, formatTime(new Date()));
        userInput.value = "";

        const indicator = showTypingIndicator();

        try {
            const streamPromise = streamMessage(language, message);

            await new Promise(resolve => setTimeout(resolve, 100));

            if (indicator && indicator.parentNode) {
                indicator.remove();
            }

            const botElement = await streamPromise;
            const fullAnswerText = botElement.textContent;

            // Append timestamp to completed response
            const timeSpan = document.createElement("span");
            timeSpan.className = "message-time";
            timeSpan.textContent = formatTime(new Date());
            botElement.appendChild(timeSpan);

            showEmailAction(message, fullAnswerText, language);

        } catch (error) {
            console.error("Chat error:", error);

            if (indicator && indicator.parentNode) {
                indicator.remove();
            }

            const bot = createBotMessage();
            bot.textContent = "Unable to connect to AyurvedaCare.\n\n"
                + "Please make sure:\n"
                + "1. Spring Boot server is running\n"
                + "2. Your AI API key (Gemini / Grok) is configured\n"
                + "3. You have an active internet connection";

        } finally {
            sendBtn.disabled = false;
            userInput.disabled = false;
            userInput.focus();
        }
    }

    // ========================================================
    // SEND BUTTON & ENTER KEY
    // ========================================================

    if (sendBtn) {
        sendBtn.addEventListener("click", sendMessage);
    }

    if (userInput) {
        userInput.addEventListener("keydown", function (event) {
            if (event.key === "Enter" && !event.shiftKey) {
                event.preventDefault();
                sendMessage();
            }
        });
    }

    // Initial fetch of user's persistent chat history
    loadChatHistory();
});
