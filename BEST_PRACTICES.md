# springboot-ai-mcp 最佳实践

在企业场景中，单独使用大模型通常会遇到两个问题：一是模型只能基于训练数据和上下文进行回答，无法直接获取实时的日志、监控、链路和仪表板信息；二是前端交互、模型调用、工具接入和链路观测如果混在一起，项目会很快变得难以扩展和排查。这个项目采用“前端轻交互、后端统一编排、按需接入 MCP、结合 DDTrace 做链路观测”的架构，就是为了解决这两个问题，让通用问答和基于真实观测数据的问答可以在同一个系统里协同工作，同时保持结构清晰、调用可追踪、问题可定位。


## 1. 背景介绍

`springboot-ai-mcp` 是一个基于 Spring Boot 3、Spring AI、Guance MCP 和 Vue 3 的 AI Agent 演示项目。它的目标不是单纯提供一个聊天接口，而是演示如何把大模型能力、观测云 MCP 工具能力和一个可直接访问的 Web UI 组合起来，形成一套可用于企业问答和可观测性排障的最小实现。


## 2. 基础架构介绍

项目采用轻量前后端一体化结构，整体链路如下：

`Vue 页面 -> /api/chat -> ChatController -> AgentService -> 纯模型回答 或 MCP 工具增强回答 -> ChatResponse`

各层职责建议保持清晰：

- `ChatController` 负责接收请求和参数校验
- `AgentService` 负责会话 ID 兜底、问题路由、模型调用和 MCP 启用
- `SpringAiAgentConfig` 负责模型与 MCP Client 装配
- `ChatRequest`、`ChatResponse` 负责统一输入输出结构

这套结构的好处是前端简单、后端职责集中，后续无论替换模型还是扩展工具，都不需要改动整体交互方式。

## 3. 观测云 MCP 接入流程

项目当前通过 `McpSyncClient` 接入观测云 MCP Server，推荐按以下流程理解和使用：

1. 在 `application.yml` 中配置观测云 MCP 地址、endpoint、API Key 和站点标识。
2. 在 `SpringAiAgentConfig` 中基于 `WebClientStreamableHttpTransport` 创建 `McpSyncClient`。
3. 在 `AgentService` 中根据用户问题内容判断是否命中观测云场景。
4. 如果命中日志、监控、仪表板、DQL、链路等关键词，则初始化 MCP Client。
5. 通过 `SyncMcpToolCallbackProvider` 将 MCP 工具注入 Spring AI 的对话调用。
6. 模型在回答过程中自动调用观测云工具获取真实数据，再组织最终回复。

当前项目的关键做法有两点：

- 非观测云问题直接走模型，不引入额外工具调用
- MCP Client 采用懒初始化，避免重复初始化和无效连接

建议保留这种“按场景启用工具”的方式，这样响应链路更清晰，也更容易排查问题。

## 4. DDTrace 接入流程

README 中已经给出了项目当前的 DDTrace 接入基础，实际流程可以概括为：

1. 启动应用时通过 `-javaagent` 注入 `dd-java-agent`。
2. 通过启动参数设置服务名、环境、版本和 trace agent 端口，例如：

```bash
-javaagent:/path/to/dd-java-agent.jar
-Ddd.service=springboot-ai-mcp
-Ddd.env=dev
-Ddd.version=0.0.1
-Ddd.trace.agent.port=9529
```

3. 在 `logback-spring.xml` 中保留以下日志字段：

- `%X{dd.service}`
- `%X{dd.trace_id}`
- `%X{dd.span_id}`

4. 应用启动后，DDTrace 会自动为 HTTP 请求、Service 调用以及底层 WebClient/Netty 请求建立链路。

这样做的价值是把接口请求、Agent 执行和 MCP 外部调用串成一条可追踪链路，便于后续在 APM 中定位慢请求、异常调用和工具访问问题。

## 5. 验证

接入完成后，建议按下面的最小步骤验证：

1. 启动项目：

```bash
mvn spring-boot:run
```

2. 打开页面：

```text
http://localhost:8088
```

3. 或直接调用接口：

```bash
curl -X POST http://localhost:8088/api/chat \
  -H 'Content-Type: application/json' \
  -d '{
    "message": "帮我查询观测云最近的日志情况",
    "conversationId": ""
  }'
```

4. 检查验证结果：

- 页面能正常发送和接收消息
- 返回体中包含 `conversationId`、`provider`、`model`、`reply`
- 日志中能看到请求进入、路由决策、MCP 初始化和处理完成记录
- 若启用了 DDTrace，日志中应出现 `dd.trace_id` 和 `dd.span_id`

如果问题命中观测云关键词，日志里还应能看到“启用观测云 MCP 工具”的相关信息。

## 6. 观测云 UI 查看效果

完成调用后，可以在观测云 UI 中重点查看两类效果：

1. APM 链路

- 查看 `POST /api/chat` 请求链路
- 确认链路中包含 `ChatController -> AgentService -> WebClient(MCP)` 调用过程
- 观察每个节点的耗时和异常情况

2. 日志关联

- 根据 `dd.trace_id` 或 `conversationId` 检索日志
- 查看本次请求是否走了纯模型回答，还是走了 MCP 工具增强回答
- 确认 MCP 初始化、工具调用和最终返回是否完整

如果链路和日志都能对齐，说明项目的“聊天入口 + Agent 编排 + MCP 工具 + Trace 关联”已经基本打通。

## 7. 总结

这个项目的核心价值是用一套很轻的 Spring Boot 架构，把大模型问答、观测云 MCP 工具调用和前端聊天交互串起来。它适合作为企业 AI Agent 的入门模板，尤其适合日志、监控、链路、DQL 等可观测性场景。实践上建议继续保持当前的分层方式、按需启用 MCP 的策略，以及 DDTrace 与日志关联的观测能力；同时尽快把配置中的明文密钥改为环境变量注入，这会更接近真实可落地的工程形态。
