package com.example.springbootaimcp.dto;

public class ChatResponse {

    private String conversationId;
    private String provider;
    private String model;
    private String reply;

    public ChatResponse() {
    }

    public ChatResponse(String conversationId, String provider, String model, String reply) {
        this.conversationId = conversationId;
        this.provider = provider;
        this.model = model;
        this.reply = reply;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }
}
