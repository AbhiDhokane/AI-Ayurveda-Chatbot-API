package com.ayurveda.bot.model;

public class EmailTreatmentRequest {
    private String email;
    private String question;
    private String answer;
    private String language;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
