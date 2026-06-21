/**
 * 模块：AI基础设施
 * 功能：LangChain4j配置，初始化 OpenAI ChatLanguageModel 和 StreamingChatLanguageModel
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码，支持 OpenAI / Ollama 双 Provider
 *  2026-06-17 简化为仅 OpenAI，API Key 从环境变量读取
 *  2026-06-17 新增 baseUrl 支持代理/兼容端点
 */
package org.example.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "langchain4j.openai.model-name")
public class LangChain4jConfig {

    private static final Logger log = LoggerFactory.getLogger(LangChain4jConfig.class);

    private String getApiKey() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("环境变量 OPENAI_API_KEY 未设置");
        }
        return apiKey;
    }

    @Bean
    @ConditionalOnProperty(name = "langchain4j.openai.model-name")
    public ChatLanguageModel chatLanguageModel(
            @Value("${langchain4j.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${langchain4j.openai.model-name}") String modelName,
            @Value("${langchain4j.openai.temperature}") double temperature,
            @Value("${langchain4j.openai.max-tokens}") int maxTokens) {
        log.info("初始化 OpenAI ChatLanguageModel, url={}, model={}", baseUrl, modelName);
        return OpenAiChatModel.builder()
                .apiKey(getApiKey()).baseUrl(baseUrl).modelName(modelName)
                .temperature(temperature).maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(120)).build();
    }

    @Bean
    @ConditionalOnProperty(name = "langchain4j.openai.model-name")
    public StreamingChatLanguageModel streamingChatLanguageModel(
            @Value("${langchain4j.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${langchain4j.openai.model-name}") String modelName,
            @Value("${langchain4j.openai.temperature}") double temperature,
            @Value("${langchain4j.openai.max-tokens}") int maxTokens) {
        log.info("初始化 OpenAI StreamingChatLanguageModel, url={}, model={}", baseUrl, modelName);
        return OpenAiStreamingChatModel.builder()
                .apiKey(getApiKey()).baseUrl(baseUrl).modelName(modelName)
                .temperature(temperature).maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(300)).build();
    }
}
