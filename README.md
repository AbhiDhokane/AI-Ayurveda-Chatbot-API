# AyurvedaCare AI 🌿

AyurvedaCare AI is a full-stack Java 17 + Spring Boot web application that combines a cloud AI assistant (Google Gemini & xAI Grok) with an Ayurvedic herb encyclopedia, user authentication, and SMTP email delivery.

---

## ✨ Features

- 🔐 **User Authentication:** Signup, login and logout with BCrypt password hashing.
- 👤 **Persistent Data:** Users and sessions stored in H2 database.
- 🤖 **Cloud AI Chatbot:** Powered by Google Gemini (Free Tier) & xAI Grok API with real-time SSE streaming.
- 🌿 **Ayurvedic Herb Encyclopedia:** Curated botanical encyclopedia loaded from `herbs.json`.
- 🔎 **Herb Search & Categories:** Filter herbs by category, health benefits, and search terms.
- 🌐 **Multilingual AI:** Accurate Ayurvedic responses in **English**, **Hindi (हिंदी)**, and **Marathi (मराठी)**.
- 📧 **Email Delivery:** Send AI wellness suggestions directly to user email via SMTP.
- 🛡️ **Ayurvedic Domain Filtering:** Automatic keyword and Levenshtein fuzzy matching ensuring questions stay on health & natural wellness.
- 🚀 **Cloud-Ready:** Dockerfile and `render.yaml` included for instant cloud hosting on Render, Railway, or VPS.

> **Medical Disclaimer:** AyurvedaCare is an educational information tool. AI output must not be treated as a clinical diagnosis or treatment plan. Always consult a qualified physician or Ayurvedic doctor for medical concerns.

---

## 🧰 Technology Stack

- **Java 17+**
- **Spring Boot 3.2.5**
- **Google Gemini API** (Gemini 3.8 / Flash - Free tier)
- **xAI Grok API** (Grok 2)
- **Thymeleaf & Modern CSS/JS**
- **Spring Data JPA & H2 Database**
- **Spring Mail (JavaMailSender / SMTP)**
- **Docker & Maven**

---

## 📁 Project Structure

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
    ├── AiChatService.java       <-- Cloud AI engine (Gemini & Grok)
    ├── AuthService.java
    ├── EmailService.java
    └── HerbService.java

src/main/resources
├── application.properties
├── herbs.json
├── static/
│   ├── css/style.css
│   └── js/chat.js
└── templates/
    ├── index.html
    ├── login.html
    ├── signup.html
    ├── herbs.html
    ├── herb-detail.html
    ├── chat.html
    └── about.html

Deployment
├── Dockerfile                   <-- Multi-stage Docker build
├── .dockerignore
└── render.yaml                  <-- 1-click cloud configuration
```

---

## ⚙️ Configuration (.env / application.properties)

All settings can be configured via environment variables or in `.env`:

```properties
# Cloud Server Port (assigned dynamically by cloud hosts)
PORT=8081

# Primary AI Provider: "gemini" (100% Free) or "grok"
AI_PROVIDER=gemini

# Google Gemini Free API Key
GEMINI_API_KEY=your_gemini_api_key_here
GEMINI_MODEL=gemini-flash-latest

# xAI Grok API Key
GROK_API_KEY=your_grok_api_key_here
GROK_MODEL=grok-2-latest

# Optional SMTP Email Delivery (Gmail App Password)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
```

---

## 🚀 Running Locally

### 1. Requirements
- Java 17 or higher (`java -version`)
- Apache Maven 3.8+ (`mvn -version`)

### 2. Run with Maven
```powershell
mvn spring-boot:run
```

### 3. Open in Browser
Open: [http://localhost:8081](http://localhost:8081)

1. Register an account at `/signup` or log in at `/login`.
2. Navigate to **Chat** to start interacting with the Ayurvedic AI.
3. Browse herbs under **Herbs** encyclopedia.

---

## 🌐 How to Host the Website Online

You can easily host this project for **free** on **Render.com**, **Railway.app**, or any Docker host.

### Option A: Hosting on Render.com (Recommended & Free)

1. Push your updated code to your GitHub repository (`https://github.com/AbhiDhokane/AI-Ayurveda-Chatbot-API`).
   ```powershell
   git add .
   git commit -m "Migrate to Gemini & Grok AI and add cloud deployment files"
   git push origin main
   ```
2. Go to [Render.com](https://render.com) and create a free account.
3. In your Render Dashboard, click **New +** and select **Web Service**.
4. Connect your GitHub repository (`AI-Ayurveda-Chatbot-API`).
5. Choose **Docker** as the Runtime (Render will automatically detect the included `Dockerfile`).
6. Set the Environment Variables under **Environment**:
   - `AI_PROVIDER`: `gemini`
   - `GEMINI_API_KEY`: `your_gemini_api_key_here`
   - `GEMINI_MODEL`: `gemini-flash-latest`
7. Click **Create Web Service**.
8. Render will build the Docker container and give you a live URL (e.g. `https://ayurvedacare-ai.onrender.com`).

---

### Option B: Hosting on Railway.app

1. Go to [Railway.app](https://railway.app).
2. Click **New Project** -> **Deploy from GitHub repo**.
3. Select `AI-Ayurveda-Chatbot-API`.
4. Under **Variables**, add:
   - `GEMINI_API_KEY`: `your_gemini_api_key_here`
   - `AI_PROVIDER`: `gemini`
5. Railway will automatically build using the `Dockerfile` and expose your live domain!

---

### Option C: Running with Docker Locally or on VPS

Build and run the Docker image:

```bash
docker build -t ayurveda-ai-bot .
docker run -p 8081:8081 -e GEMINI_API_KEY="your_api_key" ayurveda-ai-bot
```

Access at `http://localhost:8081`.

---

## 🧪 Testing & Verification

Run tests:
```powershell
mvn test
```

Build standalone executable JAR:
```powershell
mvn clean package -DskipTests
java -jar target/ayurveda-ai-bot-1.0.0.jar
```
