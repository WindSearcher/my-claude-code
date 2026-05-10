package com.windsearcher.llm.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.windsearcher.domain.ChatMessage;
import com.windsearcher.domain.ChatRequest;
import com.windsearcher.domain.ChatResponse;
import com.windsearcher.domain.Role;
import com.windsearcher.domain.ToolCall;
import com.windsearcher.exception.LlmApiException;
import com.windsearcher.llm.LlmStreamSink;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiProviderTest {

    private ObjectMapper objectMapper;
    private OpenAiProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        objectMapper = new ObjectMapper();
        provider = new OpenAiProvider(
                "openai",
                objectMapper,
                "sk-xx",
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "qwen-max",
                List.of("qwen-plus", "qwen-max")
        );
    }

//    @AfterEach
//    void tearDown() throws IOException {
//        mockWebServer.shutdown();
//    }

    // ==================== chatSync 测试 ====================

    @Test
    void chatSync_shouldReturnChatResponse() throws InterruptedException {
        ChatRequest request = ChatRequest.builder()
                .model("qwen-max")
                .providerName("openai")
                .messages(List.of(ChatMessage.builder()
                        .role(Role.USER)
                        .content("淘宝闪购是什么")
                        .build()))
                .build();

        ChatResponse response = provider.chatSync(request);

    }

//    @Test
//    void chatSync_withToolCalls_shouldParseTools() {
//        String json = """
//                {
//                  "id": "chatcmpl-456",
//                  "model": "gpt-4o",
//                  "choices": [
//                    {
//                      "message": {
//                        "role": "assistant",
//                        "content": null,
//                        "tool_calls": [
//                          {
//                            "id": "call_1",
//                            "type": "function",
//                            "function": {
//                              "name": "getWeather",
//                              "arguments": "{\\"city\\":\\"Beijing\\"}"
//                            }
//                          }
//                        ]
//                      },
//                      "finish_reason": "tool_calls"
//                    }
//                  ],
//                  "usage": {
//                    "prompt_tokens": 20,
//                    "completion_tokens": 15
//                  }
//                }
//                """;
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(200)
//                .setBody(json)
//                .addHeader("Content-Type", "application/json"));
//
//        ChatRequest request = ChatRequest.builder()
//                .model("gpt-4o")
//                .messages(List.of(ChatMessage.builder()
//                        .role(Role.USER)
//                        .content("What's the weather?")
//                        .build()))
//                .tools(List.of(Map.of(
//                        "name", "getWeather",
//                        "description", "Get weather info",
//                        "inputSchema", Map.of("type", "object", "properties", Map.of())
//                )))
//                .build();
//
//        ChatResponse response = provider.chatSync(request);
//
//        assertThat(response.getContent()).isNull();
//        assertThat(response.getFinishReason()).isEqualTo("tool_calls");
//        assertThat(response.getToolCalls()).hasSize(1);
//        ToolCall tc = response.getToolCalls().get(0);
//        assertThat(tc.getId()).isEqualTo("call_1");
//        assertThat(tc.getName()).isEqualTo("getWeather");
//        assertThat(tc.getArgumentsRaw()).isEqualTo("{\"city\":\"Beijing\"}");
//        assertThat(tc.getArguments()).isNotNull();
//        assertThat(tc.getArguments().path("city").asText()).isEqualTo("Beijing");
//    }
//
//    @Test
//    void chatSync_http4xx_shouldThrowNonRetryableException() {
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(400)
//                .setBody("{\"error\":{\"message\":\"Invalid request\"}}")
//                .addHeader("Content-Type", "application/json"));
//
//        ChatRequest request = ChatRequest.builder()
//                .model("gpt-4o")
//                .messages(List.of(ChatMessage.builder()
//                        .role(Role.USER)
//                        .content("Hi")
//                        .build()))
//                .build();
//
//        assertThatThrownBy(() -> provider.chatSync(request))
//                .isInstanceOf(LlmApiException.class)
//                .satisfies(e -> {
//                    LlmApiException ex = (LlmApiException) e;
//                    assertThat(ex.isRetryable()).isFalse();
//                    assertThat(ex.getHttpStatus()).isEqualTo(400);
//                    assertThat(ex.getMessage()).contains("chatSync HTTP 400");
//                });
//    }
//
//    @Test
//    void chatSync_http5xx_shouldThrowRetryableException() {
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(503)
//                .setBody("Service Unavailable")
//                .addHeader("Content-Type", "text/plain"));
//
//        ChatRequest request = ChatRequest.builder()
//                .model("gpt-4o")
//                .messages(List.of(ChatMessage.builder()
//                        .role(Role.USER)
//                        .content("Hi")
//                        .build()))
//                .build();
//
//        assertThatThrownBy(() -> provider.chatSync(request))
//                .isInstanceOf(LlmApiException.class)
//                .satisfies(e -> {
//                    LlmApiException ex = (LlmApiException) e;
//                    assertThat(ex.isRetryable()).isTrue();
//                    assertThat(ex.getHttpStatus()).isEqualTo(503);
//                });
//    }
//
//    @Test
//    void chatSync_emptyBody_shouldThrowException() {
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(200)
//                .addHeader("Content-Type", "application/json"));
//
//        ChatRequest request = ChatRequest.builder()
//                .model("gpt-4o")
//                .messages(List.of(ChatMessage.builder()
//                        .role(Role.USER)
//                        .content("Hi")
//                        .build()))
//                .build();
//
//        assertThatThrownBy(() -> provider.chatSync(request))
//                .isInstanceOf(LlmApiException.class)
//                .hasMessageContaining("Empty chatSync response");
//    }
//
//    @Test
//    void chatSync_shouldUseDefaultModel_whenModelNotProvided() throws InterruptedException {
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(200)
//                .setBody("""
//                        {
//                          "id": "chatcmpl-789",
//                          "model": "gpt-4o",
//                          "choices": [
//                            {
//                              "message": { "role": "assistant", "content": "OK" },
//                              "finish_reason": "stop"
//                            }
//                          ],
//                          "usage": { "prompt_tokens": 1, "completion_tokens": 1 }
//                        }
//                        """)
//                .addHeader("Content-Type", "application/json"));
//
//        ChatRequest request = ChatRequest.builder()
//                .messages(List.of(ChatMessage.builder()
//                        .role(Role.USER)
//                        .content("Hi")
//                        .build()))
//                .build();
//
//        ChatResponse response = provider.chatSync(request);
//        assertThat(response.getContent()).isEqualTo("OK");
//
//        RecordedRequest recorded = mockWebServer.takeRequest();
//        String body = recorded.getBody().readUtf8();
//        assertThat(body).contains("\"model\":\"gpt-4o\"");
//    }
//
//    // ==================== streamChat 测试 ====================
//
//    @Test
//    void streamChat_shouldAggregateSseIntoChatResponse() {
//        String sse = """
//                data: {"id":"stream-1","model":"gpt-4o","choices":[{"index":0,"delta":{"role":"assistant","content":"Hello"},"finish_reason":null}]}
//                \n""" + """
//                data: {"id":"stream-1","model":"gpt-4o","choices":[{"index":0,"delta":{"content":", world!"},"finish_reason":null}]}
//                \n""" + """
//                data: {"id":"stream-1","model":"gpt-4o","choices":[{"index":0,"delta":{},"finish_reason":"stop"}],"usage":{"prompt_tokens":3,"completion_tokens":4}}
//                \n""" + """
//                data: [DONE]
//                \n""";
//
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(200)
//                .setBody(sse)
//                .addHeader("Content-Type", "text/event-stream"));
//
//        ChatRequest request = ChatRequest.builder()
//                .model("gpt-4o")
//                .messages(List.of(ChatMessage.builder()
//                        .role(Role.USER)
//                        .content("Stream hello")
//                        .build()))
//                .build();
//
//        ChatResponse response = provider.streamChat(request);
//
//        assertThat(response.getId()).isEqualTo("stream-1");
//        assertThat(response.getModel()).isEqualTo("gpt-4o");
//        assertThat(response.getContent()).isEqualTo("Hello, world!");
//        assertThat(response.getFinishReason()).isEqualTo("stop");
//        assertThat(response.getUsageInputTokens()).isEqualTo(3);
//        assertThat(response.getUsageOutputTokens()).isEqualTo(4);
//    }
//
//    @Test
//    void streamChat_withReasoningContent_shouldAggregateReasoning() {
//        String sse = """
//                data: {"id":"stream-r1","model":"gpt-4o","choices":[{"index":0,"delta":{"role":"assistant","reasoning_content":"Think"},"finish_reason":null}]}
//                \n""" + """
//                data: {"id":"stream-r1","model":"gpt-4o","choices":[{"index":0,"delta":{"reasoning_content":"ing...","content":"Done"},"finish_reason":"stop"}],"usage":{"prompt_tokens":2,"completion_tokens":5}}
//                \n""" + """
//                data: [DONE]
//                \n""";
//
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(200)
//                .setBody(sse)
//                .addHeader("Content-Type", "text/event-stream"));
//
//        ChatRequest request = ChatRequest.builder()
//                .model("gpt-4o")
//                .messages(List.of(ChatMessage.builder()
//                        .role(Role.USER)
//                        .content("Reasoning test")
//                        .build()))
//                .build();
//
//        ChatResponse response = provider.streamChat(request);
//
//        assertThat(response.getContent()).isEqualTo("Done");
//        assertThat(response.getReasoningContent()).isEqualTo("Thinking...");
//    }
//
//    @Test
//    void streamChat_withToolCalls_shouldAggregateTools() {
//        String sse = """
//                data: {"id":"stream-tc","model":"gpt-4o","choices":[{"index":0,"delta":{"role":"assistant","content":null},"finish_reason":null}]}
//                \n""" + """
//                data: {"id":"stream-tc","model":"gpt-4o","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"id":"tc_1","type":"function","function":{"name":"getWeather"}}]},"finish_reason":null}]}
//                \n""" + """
//                data: {"id":"stream-tc","model":"gpt-4o","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"arguments":"{\\"ci"}}]},"finish_reason":null}]}
//                \n""" + """
//                data: {"id":"stream-tc","model":"gpt-4o","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"arguments":"ty\\":\\"BJ\\"}"}}]},"finish_reason":"tool_calls"}],"usage":{"prompt_tokens":5,"completion_tokens":10}}
//                \n""" + """
//                data: [DONE]
//                \n""";
//
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(200)
//                .setBody(sse)
//                .addHeader("Content-Type", "text/event-stream"));
//
//        ChatRequest request = ChatRequest.builder()
//                .model("gpt-4o")
//                .messages(List.of(ChatMessage.builder()
//                        .role(Role.USER)
//                        .content("Weather?")
//                        .build()))
//                .build();
//
//        ChatResponse response = provider.streamChat(request);
//
//        assertThat(response.getToolCalls()).hasSize(1);
//        ToolCall tc = response.getToolCalls().get(0);
//        assertThat(tc.getId()).isEqualTo("tc_1");
//        assertThat(tc.getName()).isEqualTo("getWeather");
//        assertThat(tc.getArgumentsRaw()).contains("BJ");
//    }
//
//    @Test
//    void streamChat_shouldNotifySink() {
//        String sse = """
//                data: {"id":"stream-sink","model":"gpt-4o","choices":[{"index":0,"delta":{"role":"assistant","content":"A"},"finish_reason":null}]}
//                \n""" + """
//                data: {"id":"stream-sink","model":"gpt-4o","choices":[{"index":0,"delta":{"content":"B"},"finish_reason":null}]}
//                \n""" + """
//                data: {"id":"stream-sink","model":"gpt-4o","choices":[{"index":0,"delta":{},"finish_reason":"stop"}],"usage":{"prompt_tokens":1,"completion_tokens":2}}
//                \n""" + """
//                data: [DONE]
//                \n""";
//
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(200)
//                .setBody(sse)
//                .addHeader("Content-Type", "text/event-stream"));
//
//        List<String> textDeltas = new ArrayList<>();
//        List<String> completes = new ArrayList<>();
//
//        LlmStreamSink sink = new LlmStreamSink() {
//            @Override
//            public void onTextDelta(String fragment) {
//                textDeltas.add(fragment);
//            }
//
//            @Override
//            public void onMessageComplete(String stopReason) {
//                completes.add(stopReason);
//            }
//        };
//
//        ChatRequest request = ChatRequest.builder()
//                .model("gpt-4o")
//                .messages(List.of(ChatMessage.builder()
//                        .role(Role.USER)
//                        .content("Sink test")
//                        .build()))
//                .streamSink(sink)
//                .build();
//
//        ChatResponse response = provider.streamChat(request);
//
//        assertThat(textDeltas).containsExactly("A", "B");
//        assertThat(completes).containsExactly("stop");
//        assertThat(response.getContent()).isEqualTo("AB");
//    }
//
//    @Test
//    void streamChat_httpError_shouldThrowException() {
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(401)
//                .setBody("Unauthorized")
//                .addHeader("Content-Type", "text/plain"));
//
//        ChatRequest request = ChatRequest.builder()
//                .model("gpt-4o")
//                .messages(List.of(ChatMessage.builder()
//                        .role(Role.USER)
//                        .content("Hi")
//                        .build()))
//                .build();
//
//        assertThatThrownBy(() -> provider.streamChat(request))
//                .isInstanceOf(LlmApiException.class)
//                .satisfies(e -> {
//                    LlmApiException ex = (LlmApiException) e;
//                    assertThat(ex.isRetryable()).isFalse();
//                    assertThat(ex.getHttpStatus()).isEqualTo(401);
//                });
//    }
//
//    @Test
//    void streamChat_withErrorInSse_shouldThrowAndNotifySink() {
//        String sse = """
//                data: {"id":"stream-err","model":"gpt-4o","choices":[{"index":0,"delta":{"content":"Hi"},"finish_reason":null}]}
//                \n""" + """
//                data: {"error":{"message":"model overloaded"}}
//                \n""";
//
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(200)
//                .setBody(sse)
//                .addHeader("Content-Type", "text/event-stream"));
//
//        List<String> errors = new ArrayList<>();
//        List<String> completes = new ArrayList<>();
//
//        LlmStreamSink sink = new LlmStreamSink() {
//            @Override
//            public void onTextDelta(String fragment) {
//            }
//
//            @Override
//            public void onMessageComplete(String stopReason) {
//                completes.add(stopReason);
//            }
//
//            @Override
//            public void onError(String message, boolean retryable) {
//                errors.add(message);
//            }
//        };
//
//        ChatRequest request = ChatRequest.builder()
//                .model("gpt-4o")
//                .messages(List.of(ChatMessage.builder()
//                        .role(Role.USER)
//                        .content("Error test")
//                        .build()))
//                .streamSink(sink)
//                .build();
//
//        assertThatThrownBy(() -> provider.streamChat(request))
//                .isInstanceOf(LlmApiException.class)
//                .hasMessageContaining("model overloaded");
//
//        assertThat(errors).containsExactly("model overloaded");
//        assertThat(completes).containsExactly("error");
//    }
//
//    // ==================== 其他测试 ====================
//
//    @Test
//    void getProviderName_shouldReturnConfiguredName() {
//        assertThat(provider.getProviderName()).isEqualTo("test-openai");
//    }
//
//    @Test
//    void getSupportedModels_shouldReturnConfiguredModels() {
//        assertThat(provider.getSupportedModels()).containsExactly("gpt-4o", "gpt-4o-mini");
//    }
//
//    @Test
//    void chatSync_shouldApplyTimeoutMs() throws InterruptedException {
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(200)
//                .setBody("""
//                        {
//                          "id": "chatcmpl-timeout",
//                          "model": "gpt-4o",
//                          "choices": [
//                            {
//                              "message": { "role": "assistant", "content": "OK" },
//                              "finish_reason": "stop"
//                            }
//                          ],
//                          "usage": { "prompt_tokens": 1, "completion_tokens": 1 }
//                        }
//                        """)
//                .addHeader("Content-Type", "application/json"));
//
//        ChatRequest request = ChatRequest.builder()
//                .model("gpt-4o")
//                .messages(List.of(ChatMessage.builder()
//                        .role(Role.USER)
//                        .content("Hi")
//                        .build()))
//                .timeoutMs(30000L)
//                .build();
//
//        provider.chatSync(request);
//        // MockWebServer 不会实际验证超时，但确保请求能正常发出
//        RecordedRequest recorded = mockWebServer.takeRequest();
//        assertThat(recorded.getMethod()).isEqualTo("POST");
//    }
//
//    @Test
//    void chatSync_withContentArray_shouldExtractText() {
//        String json = """
//                {
//                  "id": "chatcmpl-arr",
//                  "model": "gpt-4o",
//                  "choices": [
//                    {
//                      "message": {
//                        "role": "assistant",
//                        "content": [
//                          {"type": "text", "text": "Line1"},
//                          {"type": "text", "text": "Line2"}
//                        ]
//                      },
//                      "finish_reason": "stop"
//                    }
//                  ],
//                  "usage": { "prompt_tokens": 2, "completion_tokens": 3 }
//                }
//                """;
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(200)
//                .setBody(json)
//                .addHeader("Content-Type", "application/json"));
//
//        ChatRequest request = ChatRequest.builder()
//                .model("gpt-4o")
//                .messages(List.of(ChatMessage.builder()
//                        .role(Role.USER)
//                        .content("Array content test")
//                        .build()))
//                .build();
//
//        ChatResponse response = provider.chatSync(request);
//        assertThat(response.getContent()).isEqualTo("Line1\nLine2");
//    }
}
