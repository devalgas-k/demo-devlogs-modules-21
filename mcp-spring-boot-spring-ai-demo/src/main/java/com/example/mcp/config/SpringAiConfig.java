package com.example.mcp.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring AI configuration for multi-provider LLM support.
 * Configures OpenAI (primary), Anthropic (fallback), and Ollama (local) chat models.
 */
@Configuration
public class SpringAiConfig {

    // ==========================================================================
    // OpenAI Configuration
    // ==========================================================================

    @Value("${spring.ai.openai.api-key:}")
    private String openaiApiKey;

    @Value("${spring.ai.openai.base-url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    @Value("${spring.ai.openai.chat-model:gpt-4o}")
    private String openaiChatModel;

    @Value("${spring.ai.openai.timeout:60s}")
    private org.springframework.util.unit.DataSize openaiTimeout;

    // ==========================================================================
    // Anthropic Configuration
    // ==========================================================================

    @Value("${spring.ai.anthropic.api-key:}")
    private String anthropicApiKey;

    @Value("${spring.ai.anthropic.base-url:https://api.anthropic.com}")
    private String anthropicBaseUrl;

    @Value("${spring.ai.anthropic.chat-model:claude-3-5-sonnet-20240620}")
    private String anthropicChatModel;

    // ==========================================================================
    // Ollama Configuration (Local)
    // ==========================================================================

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.chat-model:llama3}")
    private String ollamaChatModel;

    // ==========================================================================
    // ChatModel Beans
    // ==========================================================================

    /**
     * OpenAI ChatModel bean (primary provider).
     */
    @Bean
    @Primary
    public OpenAiChatModel openAiChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(openaiApiKey)
                .baseUrl(openaiBaseUrl)
                .defaultChatOptions(options -> options
                        .model(openaiChatModel)
                        .timeout(org.springframework.http.client.reactive.ReactorClientHttpConnector.DEFAULT_TIMEOUT)
                )
                .build();
    }

    /**
     * Anthropic ChatModel bean (fallback provider).
     */
    @Bean
    public AnthropicChatModel anthropicChatModel() {
        return AnthropicChatModel.builder()
                .apiKey(anthropicApiKey)
                .baseUrl(anthropicBaseUrl)
                .defaultOptions(options -> options
                        .model(anthropicChatModel)
                )
                .build();
    }

    /**
     * Ollama ChatModel bean (local, free).
     */
    @Bean
    public OllamaChatModel ollamaChatModel() {
        return OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .defaultOptions(options -> options
                        .model(ollamaChatModel)
                )
                .build();
    }

    // ==========================================================================
    // ChatClient Beans
    // ==========================================================================

    /**
     * Primary ChatClient using OpenAI.
     */
    @Bean
    @Primary
    public ChatClient openAiChatClient(OpenAiChatModel chatModel) {
        return ChatClient.create(chatModel);
    }

    /**
     * ChatClient using Anthropic.
     */
    @Bean
    public ChatClient anthropicChatClient(AnthropicChatModel chatModel) {
        return ChatClient.create(chatModel);
    }

    /**
     * ChatClient using Ollama (local).
     */
    @Bean
    public ChatClient ollamaChatClient(OllamaChatModel chatModel) {
        return ChatClient.create(chatModel);
    }
}
