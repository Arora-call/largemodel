/**
 * 模块：AI代码生成 - 执行器模式
 * 功能：统一代码生成执行入口，根据生成模式路由到对应策略
 * 作者：yx
 * 创建时间：2026-06-24
 */
package org.example.service.ai;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.request.GenerateCodeRequest;
import org.example.enums.GenerateMode;
import org.example.service.ai.strategy.CodeGenerationStrategy;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 代码生成执行器 — 执行器模式。
 * <p>
 * 作为统一入口，根据请求中的生成模式（{@link GenerateMode}）
 * 路由到对应的 {@link CodeGenerationStrategy} 实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeGenerationExecutor {

    private final List<CodeGenerationStrategy> strategyList;
    private final DynamicModelProvider modelProvider;
    private final PromptTemplateService promptService;
    private final org.example.repository.ConversationRepository conversationRepo;
    private final org.example.repository.MessageRepository messageRepo;
    private final org.example.mapper.ProjectFileMapper projectFileMapper;
    private final org.example.service.MonitorService monitorService;
    private final org.example.mapper.ModelConfigMapper modelConfigMapper;
    private final org.example.service.DeployService deployService;
    private final KnowledgeContextService knowledgeContextService;

    private final Map<GenerateMode, CodeGenerationStrategy> registry = new EnumMap<>(GenerateMode.class);

    @PostConstruct
    void init() {
        for (CodeGenerationStrategy s : strategyList) {
            registry.put(s.supportedMode(), s);
            // 注入共享依赖到抽象基类
            if (s instanceof org.example.service.ai.strategy.AbstractCodeGenerationStrategy abs) {
                abs.setDependencies(modelProvider, promptService, conversationRepo,
                        messageRepo, projectFileMapper, monitorService, modelConfigMapper, deployService);
            }
            log.info("注册代码生成策略: {} -> {}", s.supportedMode(), s.getClass().getSimpleName());
        }
    }

    /**
     * 执行代码生成。
     *
     * @param request 生成请求（mode 字段决定使用哪种策略）
     * @param userId  当前用户 ID
     * @return SSE 流式发射器
     */
    public SseEmitter execute(GenerateCodeRequest request, Long userId) {
        GenerateMode mode = resolveMode(request);
        CodeGenerationStrategy strategy = registry.get(mode);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的生成模式: " + mode
                    + "，可用模式: " + registry.keySet());
        }

        // ── RAG: 构建知识库上下文 ──
        String kbContext = "";
        if (request.getKnowledgeDocIds() != null && !request.getKnowledgeDocIds().isEmpty()) {
            // 用户手动选择了文档
            kbContext = knowledgeContextService.buildContext(request.getKnowledgeDocIds());
        } else if (request.isAutoSearchKnowledge()) {
            // 根据 Prompt 自动检索
            kbContext = knowledgeContextService.buildContextByPrompt(userId, request.getPrompt(), 5);
        }
        if (!kbContext.isEmpty()) {
            request.setKnowledgeContext(kbContext);
        }

        log.info("执行代码生成: mode={}, prompt={}, userId={}, kbDocs={}, kbChars={}",
                mode, truncate(request.getPrompt(), 50), userId,
                request.getKnowledgeDocIds() != null ? request.getKnowledgeDocIds().size() : 0,
                kbContext.length());
        return strategy.generate(request, userId);
    }

    /** 从请求中解析生成模式（兼容旧 NATIVE/ENGINEERING 字段） */
    private GenerateMode resolveMode(GenerateCodeRequest request) {
        String type = request.getType();
        if (type == null || type.isBlank()) {
            return GenerateMode.SINGLE_FILE;
        }
        // 尝试按新枚举名解析
        try {
            return GenerateMode.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 兼容旧值
            return GenerateMode.fromLegacyType(type);
        }
    }

    private String truncate(String s, int max) {
        return s == null ? "新对话" : s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
