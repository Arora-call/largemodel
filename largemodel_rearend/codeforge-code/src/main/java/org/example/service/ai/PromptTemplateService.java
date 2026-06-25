/**
 * 模块：AI代码生成
 * 功能：Prompt 模板服务 — 从 classpath 资源文件加载提示词，支持热更新
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 *  2026-06-24 重构：移除后端语言支持，聚焦纯前端
 *  2026-06-24 重构：提示词外部化到 resources/prompt/*.txt，新增结构化输出解析
 */
package org.example.service.ai;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PromptTemplateService {

    private String singleFilePrompt;
    private String multiFilePrompt;
    private String vueProjectPrompt;

    @PostConstruct
    void init() {
        singleFilePrompt = loadPrompt("prompt/single-file-system-prompt.txt");
        multiFilePrompt = loadPrompt("prompt/multi-file-system-prompt.txt");
        vueProjectPrompt = loadPrompt("prompt/vue-project-system-prompt.txt");
        log.info("提示词加载完成: single={}, multi={}, vue={}",
                singleFilePrompt.length(), multiFilePrompt.length(), vueProjectPrompt.length());
    }

    private String loadPrompt(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("加载提示词文件失败: {}", path, e);
            return ""; // 调用方检查空字符串
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 三种模式 System Prompt（从资源文件加载）
    // ═══════════════════════════════════════════════════════════

    public String getSingleFileSystemPrompt() { return singleFilePrompt; }
    public String getMultiFileSystemPrompt() { return multiFilePrompt; }
    public String getVueProjectPromptSystemPrompt() { return vueProjectPrompt; }

    // ═══════════════════════════════════════════════════════════
    // 兼容旧接口
    // ═══════════════════════════════════════════════════════════

    public String getProjectSystemPrompt() { return vueProjectPrompt; }

    public String getCodeModifySystemPrompt(String existingCode) {
        return """
                你是一位代码编辑专家。用户选定了页面上的一个具体元素，要求你修改它。

                完整代码：
                ```
                %s
                ```

                核心规则：
                1. 你只能修改选中元素相关的代码，其余代码原样保留
                2. 输出修改后的完整代码，用```语言```代码块包裹
                3. 先写一句话说明你改了什么，然后输出代码块
                """.formatted(existingCode);
    }

    public String getProjectModifySystemPrompt(String concatenatedFiles) {
        return """
                你是代码编辑专家。下面是一个前端项目的文件列表。

                [TARGET FILE] = 优先修改的文件
                [CONTEXT ONLY — DO NOT MODIFY] = 仅供了解结构，绝对不能修改

                %s

                修改后输出格式：每个被修改的文件用 ```语言 // File: 路径 代码 ``` 格式输出。
                """.formatted(concatenatedFiles);
    }

    public String getCodeReviewSystemPrompt(String dimensions) {
        String dims = (dimensions != null && !dimensions.isBlank())
                ? dimensions : "security,performance,style,best_practice";
        return """
                你是资深代码审查专家。请审查以下代码，按以下格式输出报告。

                审查维度：%s

                ## 总体评分: [0-100分]
                ## 审查摘要
                ## 安全问题 | ## 性能问题 | ## 代码规范 | ## 最佳实践

                每个问题标注严重程度：[严重]/[中等]/[建议]
                """.formatted(dims);
    }

    public List<ChatMessage> buildMessages(String systemPrompt, String userPrompt,
                                            List<ChatMessage> history, boolean isEngineering) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));
        if (history != null && !history.isEmpty()) {
            messages.addAll(history);
        }
        String finalPrompt = isEngineering
                ? "[FORMAT: 严格按系统指令中的 JSON 格式输出。每个文件的代码必须完整可运行。]\n\n" + userPrompt
                : userPrompt;
        messages.add(UserMessage.from(finalPrompt));
        return messages;
    }
}
