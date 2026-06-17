/**
 * 模块：AI代码生成
 * 功能：Prompt模板服务，管理不同生成场景的System Prompt，构建最终发送给LLM的消息
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.service.ai;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PromptTemplateService {

    /** 原生应用生成的 System Prompt */
    public String getNativeAppSystemPrompt(String language) {
        return """
                你是一位资深全栈工程师。根据用户需求生成可运行的 %s 代码应用。

                输出格式：
                1. 先写一段简短说明（1-3句话），概括你做了什么
                2. 然后用```%s ... ```代码块包裹完整代码
                3. 代码完整可运行，包含所有 import 和依赖
                4. 代码结构清晰，关键逻辑有中文注释

                注意：不要输出代码块之外的代码片段，所有代码必须在一个代码块内。
                """.formatted(language, language);
    }

    /** 工程项目生成的 System Prompt */
    public String getEngineeringProjectSystemPrompt() {
        return """
                你是一位软件架构师。根据用户需求，规划并生成完整的工程项目。

                输出格式：
                1. 先写一段项目概述（1-3句话）
                2. 列出项目文件结构（目录树）
                3. 为每个文件生成完整代码，用 ```语言 文件名``` 格式包裹
                4. 包含必要的配置文件（pom.xml / package.json 等）
                """;
    }

    /** 对话式代码修改的 System Prompt */
    public String getCodeModifySystemPrompt(String existingCode) {
        return """
                你是一位代码编辑专家。用户选定了页面上的一个具体元素，要求你修改它。

                完整代码：
                ```
                %s
                ```

                核心规则（必须遵守）：
                1. 用户会告诉你：选中了什么元素 + 要做什么修改
                2. 你只能修改这个选中元素相关的代码，其余代码原样保留
                3. 输出修改后的完整代码，用```语言```代码块包裹
                4. 先写一句话说明你改了什么，然后输出代码块

                牢记：不相关的代码绝对不要动！
                """.formatted(existingCode);
    }

    /** 构建消息列表（含多轮历史） */
    public List<ChatMessage> buildMessages(String systemPrompt, String userPrompt,
                                            List<dev.langchain4j.data.message.ChatMessage> history) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));

        if (history != null && !history.isEmpty()) {
            messages.addAll(history);
        }

        messages.add(UserMessage.from(userPrompt));
        return messages;
    }
}
