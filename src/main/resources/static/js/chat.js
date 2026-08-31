document.addEventListener(
    "DOMContentLoaded",
    function () {

        const messagesContainer =
            document.getElementById(
                "chat-messages"
            );

        const userInput =
            document.getElementById(
                "user-input"
            );

        const sendBtn =
            document.getElementById(
                "send-btn"
            );

        const languageSelect =
            document.getElementById(
                "language"
            );

        const emailPanel = document.getElementById("email-panel");


        // ========================================================
        // USER MESSAGE
        // ========================================================

        function addUserMessage(message) {

            const messageDiv =
                document.createElement("div");

            messageDiv.className =
                "message user";


            const content =
                document.createElement("div");

            content.className =
                "message-content";

            content.textContent =
                message;


            messageDiv.appendChild(
                content
            );


            messagesContainer.appendChild(
                messageDiv
            );


            scrollToBottom();
        }


        // ========================================================
        // BOT MESSAGE
        // ========================================================

        function createBotMessage() {

            const messageDiv =
                document.createElement("div");

            messageDiv.className =
                "message bot";


            const content =
                document.createElement("div");

            content.className =
                "message-content";


            messageDiv.appendChild(
                content
            );


            messagesContainer.appendChild(
                messageDiv
            );


            scrollToBottom();


            return content;
        }


        // ========================================================
        // TYPING INDICATOR
        // ========================================================

        function showTypingIndicator() {

            const div =
                document.createElement("div");

            div.id =
                "typing-indicator";

            div.className =
                "message bot";


            div.innerHTML = `
                <div class="typing-indicator">
                    <span></span>
                    <span></span>
                    <span></span>
                </div>
            `;


            messagesContainer.appendChild(
                div
            );


            scrollToBottom();


            return div;
        }


        // ========================================================
        // SCROLL
        // ========================================================

        function scrollToBottom() {

            messagesContainer.scrollTo({

                top:
                    messagesContainer
                        .scrollHeight,

                behavior:
                    "smooth"
            });
        }


        // ========================================================
        // TRUE STREAMING
        // ========================================================

        function streamMessage(
                language,
                message) {

            return new Promise(
                function (
                        resolve,
                        reject
                ) {

                    const botElement =
                        createBotMessage();


                    const url =
                        "/api/chat/stream"
                        + "?language="
                        + encodeURIComponent(
                            language
                        )
                        + "&message="
                        + encodeURIComponent(
                            message
                        );


                    /*
                     * Open SSE.
                     */
                    const eventSource =
                        new EventSource(
                            url
                        );


                    let finished = false;


                    // =================================================
                    // RECEIVE CHUNK
                    // =================================================

                    eventSource.onmessage =
                        function (event) {

                            if (finished) {
                                return;
                            }


                            try {

                                /*
                                 * Backend sends JSON encoded
                                 * chunks.
                                 *
                                 * Example:
                                 *
                                 * "Hello world "
                                 */
                                const chunk =
                                    JSON.parse(
                                        event.data
                                    );


                                /*
                                 * IMPORTANT:
                                 *
                                 * Do NOT use:
                                 *
                                 * trim()
                                 *
                                 * because spaces are important.
                                 */
                                botElement.textContent +=
                                    chunk;


                                scrollToBottom();


                            } catch (error) {

                                console.error(
                                    "Invalid SSE chunk:",
                                    error
                                );
                            }
                        };


                    // =================================================
                    // DONE
                    // =================================================

                    eventSource.addEventListener(
                        "done",
                        function () {

                            if (finished) {
                                return;
                            }


                            finished = true;


                            eventSource.close();


                            resolve(botElement.textContent);
                        }
                    );


                    // =================================================
                    // ERROR
                    // =================================================

                    eventSource.onerror =
                        function () {

                            if (finished) {
                                return;
                            }


                            /*
                             * If the server closed the
                             * connection after sending
                             * the complete answer, treat
                             * it as successful.
                             */
                            if (
                                botElement
                                    .textContent
                                    .length > 0
                            ) {

                                finished = true;

                                eventSource.close();

                                resolve(botElement.textContent);

                                return;
                            }


                            finished = true;

                            eventSource.close();

                            reject(
                                new Error(
                                    "SSE connection failed."
                                )
                            );
                        };
                }
            );
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
            document.getElementById("treatment-email-form").addEventListener("submit", async function(event) {
                event.preventDefault();
                const status = document.getElementById("email-status");
                const email = document.getElementById("treatment-email").value.trim();
                status.textContent = "Sending...";
                try {
                    const response = await fetch("/api/email/treatment", {
                        method: "POST", headers: {"Content-Type":"application/json"},
                        body: JSON.stringify({email, question, answer, language})
                    });
                    const data = await response.json();
                    status.textContent = data.message || "Done.";
                } catch (error) { status.textContent = "Unable to send email. Check your SMTP configuration."; }
            });
        }

        // ========================================================
        // SEND
        // ========================================================

        async function sendMessage() {

            const message =
                userInput.value.trim();


            if (!message) {
                return;
            }


            const language =
                languageSelect.value ||
                "English";


            /*
             * Disable controls.
             */
            sendBtn.disabled = true;

            userInput.disabled = true;


            /*
             * Show user message.
             */
            addUserMessage(
                message
            );


            /*
             * Clear input.
             */
            userInput.value = "";


            /*
             * Show dots.
             */
            const indicator =
                showTypingIndicator();


            try {

                /*
                 * Start streaming.
                 */
                const streamPromise =
                    streamMessage(
                        language,
                        message
                    );


                /*
                 * Give browser a moment
                 * to create connection.
                 */
                await new Promise(
                    resolve =>
                        setTimeout(
                            resolve,
                            100
                        )
                );


                /*
                 * Remove dots.
                 */
                if (
                    indicator
                    &&
                    indicator.parentNode
                ) {

                    indicator.remove();
                }


                /*
                 * Wait until Qwen
                 * finishes.
                 */
                const finalAnswer = await streamPromise;
                showEmailAction(message, finalAnswer, language);


            } catch (error) {

                console.error(
                    "Chat error:",
                    error
                );


                if (
                    indicator
                    &&
                    indicator.parentNode
                ) {

                    indicator.remove();
                }


                const bot =
                    createBotMessage();


                bot.textContent =
                    "Unable to connect to AyurvedaCare.\n\n"
                    + "Please make sure:\n"
                    + "1. Ollama is running\n"
                    + "2. Qwen model is installed\n"
                    + "3. Spring Boot server is running";


            } finally {

                sendBtn.disabled = false;

                userInput.disabled = false;

                userInput.focus();
            }
        }


        // ========================================================
        // SEND BUTTON
        // ========================================================

        if (sendBtn) {

            sendBtn.addEventListener(
                "click",
                sendMessage
            );
        }


        // ========================================================
        // ENTER KEY
        // ========================================================

        if (userInput) {

            userInput.addEventListener(
                "keydown",
                function (event) {

                    if (
                        event.key === "Enter"
                        &&
                        !event.shiftKey
                    ) {

                        event.preventDefault();

                        sendMessage();
                    }
                }
            );
        }

    }
);