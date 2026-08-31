package com.ayurveda.bot.model;

public class ChatResponse {

    private String reply;

    private String language;

    private boolean success;

    private String error;


    public ChatResponse() {
    }


    public ChatResponse(
            String reply,
            String language,
            boolean success) {

        this.reply = reply;

        this.language = language;

        this.success = success;

        this.error = null;
    }


    public static ChatResponse error(
            String message) {

        ChatResponse response =
                new ChatResponse();

        response.reply = "";

        response.language = "English";

        response.success = false;

        response.error = message;

        return response;
    }


    public String getReply() {
        return reply;
    }


    public void setReply(
            String reply) {

        this.reply = reply;
    }


    public String getLanguage() {
        return language;
    }


    public void setLanguage(
            String language) {

        this.language = language;
    }


    public boolean isSuccess() {
        return success;
    }


    public void setSuccess(
            boolean success) {

        this.success = success;
    }


    public String getError() {
        return error;
    }


    public void setError(
            String error) {

        this.error = error;
    }
}