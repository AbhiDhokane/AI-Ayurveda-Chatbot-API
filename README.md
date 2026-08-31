# AyurvedaCare AI 🌿

AyurvedaCare AI is a Java 17 + Spring Boot web application that combines a local Ollama/Qwen AI assistant with an Ayurvedic herb encyclopedia, user authentication, and SMTP email delivery.

## ✨ Enhanced features

- 🔐 Signup, login and logout
- 🔒 BCrypt password hashing
- 👤 User email stored in an H2 database
- 🤖 Local AI chatbot powered by Ollama + Qwen
- 🌿 Ayurvedic herb encyclopedia from `herbs.json`
- 🔎 Herb search and categories
- 🌐 Multilingual AI answers
- 📧 **Send AI-generated treatment/wellness information by email** after each answer
- 🛡️ Health/Ayurveda domain restriction and medical safety instructions
- 💾 File-based H2 database for local development

> **Medical note:** AyurvedaCare is an educational information tool. AI output may be inaccurate and must not be treated as a diagnosis or personalized treatment plan. Do not stop prescribed medicines based on AI output. Seek professional medical care for serious or worsening symptoms.

## 🧰 Technology

- Java 17+
- Spring Boot 3.2.5
- Thymeleaf
- Spring Data JPA
- H2 Database
- Spring Mail / SMTP
- Spring Security Crypto (BCrypt)
- Maven
- Ollama
- Qwen 2.5 3B Instruct
- Jackson

## 📁 Project structure

```text
src/main/java/com/ayurveda/bot
├── AyurvedaAiBotApplication.java
├── controller
│   ├── AuthController.java
│   ├── ChatApiController.java
│   ├── EmailController.java
│   └── HerbController.java
├── model
│   ├── AppUser.java
│   ├── ChatRequest.java
│   ├── ChatResponse.java
│   ├── EmailTreatmentRequest.java
│   └── Herb.java
├── repository
│   └── AppUserRepository.java
└── service
    ├── AuthService.java
    ├── EmailService.java
    ├── HerbService.java
    └── OllamaChatService.java

src/main/resources
├── application.properties
├── herbs.json
├── static
│   ├── css/style.css
│   └── js/chat.js
└── templates
    ├── index.html
    ├── login.html
    ├── signup.html
    ├── herbs.html
    ├── herb-detail.html
    ├── chat.html
    └── about.html
```

## 1. Install Java

Verify Java 17 or newer:

```powershell
java -version
```

## 2. Install Maven

Verify:

```powershell
mvn -version
```

If `mvn` is not recognized on Windows, install Maven and add its `bin` directory to PATH.

## 3. Install Ollama

Install Ollama for Windows from the official Ollama website.

Verify:

```powershell
ollama --version
```

Pull the model used by this project:

```powershell
ollama pull qwen2.5:3b-instruct
```

Test it:

```powershell
ollama run qwen2.5:3b-instruct
```

Exit with:

```text
/bye
```

## 4. Configure SMTP email

The application reads SMTP credentials from environment variables. **Do not put your Gmail password directly into source code.** For Gmail, use a Google **App Password**, not your normal account password.

### PowerShell example

```powershell
$env:MAIL_HOST="smtp.gmail.com"
$env:MAIL_PORT="587"
$env:MAIL_USERNAME="yourgmail@gmail.com"
$env:MAIL_PASSWORD="your-16-character-app-password"
```

Keep that PowerShell window open while running Spring Boot.

For another SMTP provider, change `MAIL_HOST` and `MAIL_PORT` as required by that provider.

## 5. Run the application

From the project directory:

```powershell
mvn clean package
mvn spring-boot:run
```

Open:

```text
http://localhost:8081
```

Main pages:

```text
http://localhost:8081/
http://localhost:8081/login
http://localhost:8081/signup
http://localhost:8081/chat
http://localhost:8081/herbs
http://localhost:8081/about
```

## 6. First-time user flow

1. Open `/signup`.
2. Enter your name, email and a password of at least 6 characters.
3. After signup, you are logged in automatically.
4. Open the AI Chatbot.
5. Ask an Ayurveda/health-related question.
6. Wait for the Qwen answer.
7. The **Send Treatment Info** email panel appears below the answer.
8. Confirm or change the email address.
9. Click **Send Treatment Info**.
10. The application sends the AI answer and safety disclaimer through SMTP.

## 7. Authentication design

User records are stored in the local H2 database at:

```text
./data/ayurvedacare
```

Passwords are stored as BCrypt hashes rather than plain text.

For a production deployment, replace the local H2 setup with PostgreSQL/MySQL and add production-grade Spring Security features such as CSRF protection, rate limiting, email verification, password reset, secure cookies and account lockout policies.

## 8. Email API

The frontend sends the generated answer to:

```text
POST /api/email/treatment
```

Example JSON:

```json
{
  "email": "user@example.com",
  "question": "What are the traditional uses of Ashwagandha?",
  "answer": "AI-generated response...",
  "language": "English"
}
```

The endpoint requires a logged-in session and validates the recipient email.

## 9. Chat API

Normal endpoint:

```text
POST /api/chat
```

Example:

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8081/api/chat" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"language":"English","message":"What is Ashwagandha?"}' `
  -WebSession $session
```

The browser UI uses the streaming endpoint:

```text
GET /api/chat/stream?language=English&message=...
```

## 10. AI architecture

```text
Browser
   ↓
Spring Boot :8081
   ↓
Authentication / Session
   ↓
ChatApiController
   ↓
OllamaChatService
   ↓
Ollama :11434
   ↓
Qwen 2.5 3B Instruct
```

Email flow:

```text
User question
      ↓
Qwen AI answer
      ↓
Email panel appears
      ↓
User confirms email
      ↓
POST /api/email/treatment
      ↓
Spring Mail
      ↓
SMTP provider
      ↓
User inbox
```

## 11. Health/Ayurveda scope

The chatbot is intentionally focused on:

- Ayurveda
- Medicinal herbs
- Ayurvedic medicines and formulations
- General health education
- Natural wellness
- Diet and nutrition
- Yoga and Pranayama
- Meditation and sleep
- Healthy lifestyle

The service rejects unrelated topics instead of sending them to the local AI model.

## 12. Troubleshooting

### `mvn` is not recognized

Install Maven and add Maven's `bin` directory to PATH. Then reopen PowerShell and run:

```powershell
mvn -version
```

### Port 8081 is unavailable

Check whether another process is using the port, or change `server.port` in `application.properties`.

### Ollama connection error

Run:

```powershell
ollama list
```

Make sure `qwen2.5:3b-instruct` is installed and Ollama is running.

Test Ollama:

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:11434/api/tags" `
  -Method Get
```

### Email is not sent

Check all of these:

```powershell
echo $env:MAIL_USERNAME
echo $env:MAIL_HOST
echo $env:MAIL_PORT
```

For Gmail:

- SMTP host: `smtp.gmail.com`
- Port: `587`
- STARTTLS enabled
- Use a Google App Password
- Make sure the sender account is allowed to send mail

### Email panel does not appear

Make sure you are logged in and that the AI answer finishes generating. Check the browser developer console for JavaScript errors.

## 13. Important security notes

- Never commit SMTP passwords to GitHub.
- Never commit a `.env` file containing secrets.
- Use environment variables or a secret manager for deployment.
- H2 file storage is intended for local/demo use.
- Add HTTPS before production deployment.

## 14. What was enhanced

Compared with the original AyurvedaCare AI application, this version adds:

- Complete signup/login/logout flow
- BCrypt password hashing
- Persistent local user storage with H2/JPA
- Session-protected AI chat
- SMTP integration using Spring Mail
- Email address collection and validation
- One-click email delivery after an AI response
- Dedicated email API
- Improved navigation and signed-in user display
- New authentication pages
- Updated project documentation

---

**AyurvedaCare AI — Ancient wisdom, modern AI, responsible wellness education.**
