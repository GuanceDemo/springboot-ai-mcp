package com.example.springbootaimcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public class AiProviderProperties {

    private String provider = "zhipu";
    private String model = "glm-4.5-air";
    private String systemPrompt = """
            你是一个面向企业可观测性场景的 Spring Boot AI Agent。
            当用户的问题涉及观测云、日志、监控器、仪表板、DQL、指标、告警、事件、跟踪、工作空间排障时，
            优先调用已接入的观测云 MCP 工具获取真实数据，再基于工具结果回答。
            如果问题与观测云无关，则直接给出简洁、准确、可执行的回答。
            回答时优先使用中文。
            """.strip();
    private final Zhipu zhipu = new Zhipu();
    private final Guance guance = new Guance();

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

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public Zhipu getZhipu() {
        return zhipu;
    }

    public Guance getGuance() {
        return guance;
    }

    public static class Zhipu {

        private String apiKey = "";
        private String baseUrl = "https://open.bigmodel.cn";
        private String chatPath = "/api/paas/v4/chat/completions";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getChatPath() {
            return chatPath;
        }

        public void setChatPath(String chatPath) {
            this.chatPath = chatPath;
        }
    }

    public static class Guance {

        private String baseUrl = "https://obsy-ai.guance.com";
        private String endpoint = "/obsy_ai_mcp/mcp";
        private String apiKey = "";
        private String siteKey = "cn1";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getSiteKey() {
            return siteKey;
        }

        public void setSiteKey(String siteKey) {
            this.siteKey = siteKey;
        }
    }
}
