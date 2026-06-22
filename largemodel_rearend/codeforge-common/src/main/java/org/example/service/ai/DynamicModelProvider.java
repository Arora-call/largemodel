/**
 * 动态 AI 模型提供器 — 支持运行时根据 modelId 切换模型
 * 缓存已创建的模型实例（按 modelId），优先使用 DB 配置，fallback 到 application.yml
 */
package org.example.service.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.ModelConfig;
import org.example.mapper.ModelConfigMapper;
import org.example.util.AesUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicModelProvider {

    private final ModelConfigMapper modelConfigMapper;

    /** Fallback config from application.yml */
    @Value("${langchain4j.openai.base-url:https://api.openai.com/v1}")
    private String fallbackBaseUrl;
    @Value("${langchain4j.openai.model-name:}")
    private String fallbackModelName;
    @Value("${langchain4j.openai.temperature:0.7}")
    private double fallbackTemperature;
    @Value("${langchain4j.openai.max-tokens:16384}")
    private int fallbackMaxTokens;

    private final Map<Long, StreamingChatLanguageModel> streamingCache = new ConcurrentHashMap<>();
    private final Map<Long, ChatLanguageModel> chatCache = new ConcurrentHashMap<>();

    /** 根据模型ID获取流式模型，如 modelId 为 null 则返回默认模型 */
    public StreamingChatLanguageModel getStreaming(Long modelId) {
        if (modelId != null) {
            return streamingCache.computeIfAbsent(modelId, this::buildStreamingFromDb);
        }
        return getDefaultStreaming();
    }

    /** 根据模型ID获取普通模型 */
    public ChatLanguageModel getChat(Long modelId) {
        if (modelId != null) {
            return chatCache.computeIfAbsent(modelId, this::buildChatFromDb);
        }
        return getDefaultChat();
    }

    /** 获取默认流式模型（优先 DB，fallback 到 yml） */
    public StreamingChatLanguageModel getDefaultStreaming() {
        ModelConfig def = modelConfigMapper.findDefault().orElse(null);
        if (def != null) {
            return streamingCache.computeIfAbsent(def.getId(), this::buildStreamingFromDb);
        }
        return buildFallbackStreaming();
    }

    /** 获取默认普通模型 */
    public ChatLanguageModel getDefaultChat() {
        ModelConfig def = modelConfigMapper.findDefault().orElse(null);
        if (def != null) {
            return chatCache.computeIfAbsent(def.getId(), this::buildChatFromDb);
        }
        return buildFallbackChat();
    }

    /** 刷新缓存（模型配置更新后调用） */
    public void evictCache(Long modelId) {
        streamingCache.remove(modelId);
        chatCache.remove(modelId);
        log.info("已清除模型缓存: modelId={}", modelId);
    }

    public void evictAll() {
        streamingCache.clear();
        chatCache.clear();
        log.info("已清除所有模型缓存");
    }

    private StreamingChatLanguageModel buildStreamingFromDb(Long id) {
        ModelConfig mc = modelConfigMapper.findEnabledById(id).orElse(null);
        if (mc == null) return buildFallbackStreaming();
        String apiKey = AesUtil.decrypt(mc.getApiKeyEncrypted());
        log.info("创建流式模型(DB): id={}, name={}, model={}", id, mc.getName(), mc.getModelName());
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey).baseUrl(mc.getBaseUrl()).modelName(mc.getModelName())
                .temperature(mc.getTemperature()).maxTokens(mc.getMaxTokens())
                .timeout(Duration.ofSeconds(300)).build();
    }

    private ChatLanguageModel buildChatFromDb(Long id) {
        ModelConfig mc = modelConfigMapper.findEnabledById(id).orElse(null);
        if (mc == null) return buildFallbackChat();
        String apiKey = AesUtil.decrypt(mc.getApiKeyEncrypted());
        return OpenAiChatModel.builder()
                .apiKey(apiKey).baseUrl(mc.getBaseUrl()).modelName(mc.getModelName())
                .temperature(mc.getTemperature()).maxTokens(mc.getMaxTokens())
                .timeout(Duration.ofSeconds(120)).build();
    }

    private StreamingChatLanguageModel buildFallbackStreaming() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("无可用模型：请配置 model_configs 或设置环境变量 OPENAI_API_KEY");
        }
        log.info("创建流式模型(fallback): model={}", fallbackModelName);
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey).baseUrl(fallbackBaseUrl).modelName(fallbackModelName)
                .temperature(fallbackTemperature).maxTokens(fallbackMaxTokens)
                .timeout(Duration.ofSeconds(300)).build();
    }

    private ChatLanguageModel buildFallbackChat() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("无可用模型：请配置 model_configs 或设置环境变量 OPENAI_API_KEY");
        }
        return OpenAiChatModel.builder()
                .apiKey(apiKey).baseUrl(fallbackBaseUrl).modelName(fallbackModelName)
                .temperature(fallbackTemperature).maxTokens(fallbackMaxTokens)
                .timeout(Duration.ofSeconds(120)).build();
    }
}
