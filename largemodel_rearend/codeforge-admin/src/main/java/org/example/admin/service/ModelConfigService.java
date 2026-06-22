/**
 * AI 模型配置服务 — CRUD + 加密/解密 + 测试连接
 */
package org.example.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.ModelConfig;
import org.example.mapper.ModelConfigMapper;
import org.example.util.AesUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelConfigService {

    private final ModelConfigMapper mapper;

    /** 列出所有已启用模型（前端用，apiKey 不返回） */
    public List<Map<String, Object>> listEnabled() {
        return mapper.findAllEnabled().stream().map(this::toSafeMap).collect(Collectors.toList());
    }

    /** 管理员：列出全部模型（含加密 Key） */
    public List<ModelConfig> listAll() {
        return mapper.selectList(null);
    }

    /** 管理员：获取单个（用于编辑，apiKey 也脱敏） */
    public Map<String, Object> getById(Long id) {
        ModelConfig mc = mapper.selectById(id);
        if (mc == null) throw new RuntimeException("模型不存在");
        Map<String, Object> m = toSafeMap(mc);
        m.put("apiKeyEncrypted", mc.getApiKeyEncrypted()); // 编辑时回显密文
        return m;
    }

    /** 添加模型 */
    @Transactional
    public ModelConfig create(String name, String provider, String baseUrl, String rawApiKey,
                               String modelName, Double temp, Integer maxTokens, Integer sortOrder) {
        // 检查是否重复
        List<ModelConfig> existing = mapper.selectList(null);
        boolean exists = existing.stream().anyMatch(m ->
                m.getBaseUrl().equals(baseUrl) && m.getModelName().equals(modelName));
        if (exists) throw new RuntimeException("该模型配置已存在");

        ModelConfig mc = ModelConfig.builder()
                .name(name).provider(provider).baseUrl(baseUrl).modelName(modelName)
                .apiKeyEncrypted(AesUtil.encrypt(rawApiKey))
                .temperature(temp != null ? temp : 0.7)
                .maxTokens(maxTokens != null ? maxTokens : 16384)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .isEnabled(1).isDefault(0)
                .build();
        mapper.insert(mc);
        log.info("新增模型配置: id={}, name={}, provider={}", mc.getId(), name, provider);
        return mc;
    }

    /** 更新模型 */
    @Transactional
    public void update(Long id, String name, String provider, String baseUrl, String rawApiKey,
                        String modelName, Double temp, Integer maxTokens, Integer sortOrder,
                        Integer isEnabled, Integer isDefault) {
        ModelConfig mc = mapper.selectById(id);
        if (mc == null) throw new RuntimeException("模型不存在");
        if (name != null) mc.setName(name);
        if (provider != null) mc.setProvider(provider);
        if (baseUrl != null) mc.setBaseUrl(baseUrl);
        if (modelName != null) mc.setModelName(modelName);
        if (temp != null) mc.setTemperature(temp);
        if (maxTokens != null) mc.setMaxTokens(maxTokens);
        if (sortOrder != null) mc.setSortOrder(sortOrder);
        if (isEnabled != null) mc.setIsEnabled(isEnabled);
        if (isDefault != null) mc.setIsDefault(isDefault);
        // 仅当用户输入了新 API Key 时才更新
        if (rawApiKey != null && !rawApiKey.isBlank() && !rawApiKey.contains("***")) {
            mc.setApiKeyEncrypted(AesUtil.encrypt(rawApiKey));
        }
        // 如果设置为默认，取消其他模型的默认
        if (isDefault != null && isDefault == 1) {
            mapper.findAllEnabled().forEach(m -> {
                if (!m.getId().equals(id) && m.getIsDefault() == 1) {
                    m.setIsDefault(0);
                    mapper.updateById(m);
                }
            });
        }
        mapper.updateById(mc);
        log.info("更新模型配置: id={}", id);
    }

    /** 删除模型 */
    @Transactional
    public void delete(Long id) {
        mapper.deleteById(id);
        log.info("删除模型配置: id={}", id);
    }

    /** 测试模型连接 — 发送最小对话请求验证 API 可用 */
    public Map<String, Object> testConnection(Long id) {
        ModelConfig mc = mapper.selectById(id);
        if (mc == null) throw new RuntimeException("模型不存在");
        String apiKey;
        try {
            apiKey = AesUtil.decrypt(mc.getApiKeyEncrypted());
        } catch (Exception e) {
            log.error("API Key 解密失败: id={}, error={}", id, e.getMessage());
            return Map.of("success", false, "error", "密钥解密失败: " + e.getMessage());
        }
        try {
            // 规范化 baseUrl：去除末尾斜杠，直接追加 /chat/completions
            String base = mc.getBaseUrl().replaceAll("/+$", "");
            // 如果 URL 已经以 /chat/completions 结尾则直接用
            String url = base.endsWith("/chat/completions") ? base : base + "/chat/completions";

            String body = """
                    {"model":"%s","messages":[{"role":"user","content":"hi"}],"max_tokens":5}"""
                    .formatted(mc.getModelName());

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(java.time.Duration.ofSeconds(15))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                    .build();
            java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            boolean ok = resp.statusCode() == 200;
            log.info("模型连接测试: id={}, url={}, status={}", id, url, resp.statusCode());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", ok);
            result.put("statusCode", resp.statusCode());
            if (!ok) {
                String errBody = resp.body();
                if (errBody.length() > 300) errBody = errBody.substring(0, 300);
                result.put("body", errBody);
            }
            return result;
        } catch (Exception e) {
            log.error("模型连接测试异常: id={}, error={}", id, e.getMessage());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /** 获取解密后的 API Key（仅内部使用） */
    public ModelConfig getDecrypted(Long id) {
        ModelConfig mc = mapper.selectById(id);
        if (mc == null) throw new RuntimeException("模型不存在: " + id);
        mc.setApiKeyEncrypted(AesUtil.decrypt(mc.getApiKeyEncrypted()));
        return mc;
    }

    /** 获取默认模型 */
    public ModelConfig getDefault() {
        return mapper.findDefault().orElse(null);
    }

    /** 转为安全 Map（加密字段脱敏） */
    private Map<String, Object> toSafeMap(ModelConfig mc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", mc.getId());
        m.put("name", mc.getName());
        m.put("provider", mc.getProvider());
        m.put("baseUrl", mc.getBaseUrl());
        m.put("modelName", mc.getModelName());
        m.put("temperature", mc.getTemperature());
        m.put("maxTokens", mc.getMaxTokens());
        m.put("isEnabled", mc.getIsEnabled());
        m.put("isDefault", mc.getIsDefault());
        m.put("sortOrder", mc.getSortOrder());
        // API Key 脱敏：sk-a***b123
        String raw = mc.getApiKeyEncrypted();
        if (raw != null && raw.length() > 8) {
            try { raw = AesUtil.mask(AesUtil.decrypt(raw)); } catch (Exception ignored) {}
        }
        m.put("apiKeyMasked", raw);
        return m;
    }
}
