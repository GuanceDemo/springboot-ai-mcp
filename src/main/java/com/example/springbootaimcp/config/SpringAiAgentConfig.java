package com.example.springbootaimcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.WebClientStreamableHttpTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class SpringAiAgentConfig {

    @Bean
    public McpSyncClient guanceMcpClient(WebClient.Builder webClientBuilder,
                                         ObjectMapper objectMapper,
                                         AiProviderProperties properties) {
        var guance = properties.getGuance();

        var transport = WebClientStreamableHttpTransport.builder(
                        webClientBuilder
                                .baseUrl(guance.getBaseUrl())
                                .defaultHeader(HttpHeaders.ACCEPT, "application/json, text/event-stream")
                                .defaultHeader(HttpHeaders.AUTHORIZATION, guance.getApiKey())
                                .defaultHeader("Endpoint", guance.getSiteKey()))
                .endpoint(guance.getEndpoint())
                .jsonMapper(new JacksonMcpJsonMapper(objectMapper))
                .build();

        var client = McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("springboot-ai-mcp", "0.0.1"))
                .build();
        return client;
    }
}
