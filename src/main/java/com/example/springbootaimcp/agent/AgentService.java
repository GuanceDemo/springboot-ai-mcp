package com.example.springbootaimcp.agent;

import com.example.springbootaimcp.config.AiProviderProperties;
import com.example.springbootaimcp.dto.ChatRequest;
import com.example.springbootaimcp.dto.ChatResponse;
import io.modelcontextprotocol.client.McpSyncClient;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    private static final List<String> OBSERVABILITY_KEYWORDS = List.of(
            "观测云", "guance", "日志", "log", "监控", "monitor", "告警", "alert",
            "事件", "event", "指标", "metric", "链路", "trace", "span",
            "apm", "rum", "仪表板", "dashboard", "工作空间", "workspace",
            "dql", "查询", "主机", "容器", "pod", "k8s", "kubernetes"
    );

    private final ChatClient chatClient;
    private final McpSyncClient guanceMcpClient;
    private final AiProviderProperties properties;

    public AgentService(ChatClient.Builder chatClientBuilder,
                        McpSyncClient guanceMcpClient,
                        AiProviderProperties properties) {
        this.chatClient = chatClientBuilder.build();
        this.guanceMcpClient = guanceMcpClient;
        this.properties = properties;
    }

    public CompletableFuture<ChatResponse> dispatch(ChatRequest request) {
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        String finalConversationId = conversationId;
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始处理Agent请求, conversationId={}, provider={}, model={}",
                        finalConversationId, properties.getProvider(), properties.getModel());
                boolean useMcp = shouldUseMcp(request.getMessage());
                log.info("Agent路由决策, conversationId={}, useMcp={}", finalConversationId, useMcp);

                String reply;
                if (useMcp) {
                    initializeGuanceClientIfNecessary();
                    log.info("启用观测云MCP工具, conversationId={}", finalConversationId);
                    reply = this.chatClient.prompt()
                            .system(properties.getSystemPrompt())
                            .user(request.getMessage())
                            .toolCallbacks(new SyncMcpToolCallbackProvider(guanceMcpClient))
                            .call()
                            .content();
                }
                else {
                    log.info("直接调用模型，不启用MCP工具, conversationId={}", finalConversationId);
                    reply = this.chatClient.prompt()
                            .system(properties.getSystemPrompt())
                            .user(request.getMessage())
                            .call()
                            .content();
                }

                log.info("Agent处理完成, conversationId={}, replyLength={}",
                        finalConversationId, reply == null ? 0 : reply.length());

                return new ChatResponse(
                        finalConversationId,
                        properties.getProvider(),
                        properties.getModel(),
                        reply
                );
            }
            catch (Exception ex) {
                log.error("Agent处理失败, conversationId={}", finalConversationId, ex);
                return new ChatResponse(
                        finalConversationId,
                        properties.getProvider(),
                        properties.getModel(),
                        buildFailureMessage(ex)
                );
            }
        });
    }

    private void initializeGuanceClientIfNecessary() {
        if (!StringUtils.hasText(properties.getGuance().getApiKey())) {
            log.warn("未配置观测云MCP API Key，将跳过MCP初始化");
            return;
        }
        if (!guanceMcpClient.isInitialized()) {
            synchronized (guanceMcpClient) {
                if (!guanceMcpClient.isInitialized()) {
                    log.info("开始初始化观测云MCP客户端, endpoint={}, siteKey={}",
                            properties.getGuance().getEndpoint(), properties.getGuance().getSiteKey());
                    guanceMcpClient.initialize();
                    log.info("观测云MCP客户端初始化完成, serverInfo={}", guanceMcpClient.getServerInfo());
                }
            }
        }
    }

    private boolean shouldUseMcp(String message) {
        if (!StringUtils.hasText(message)) {
            return false;
        }
        String normalized = message.toLowerCase();
        return OBSERVABILITY_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    private String buildFailureMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }
        return "Agent 调用失败：" + message;
    }
}
