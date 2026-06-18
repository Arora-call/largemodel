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
                你是资深全栈工程师，根据需求生成可运行的 %s 代码应用。

                === 输出格式（严格遵守，否则解析失败） ===

                先用1-3句话简述你做了什么（纯文本，不要用反引号包裹）。
                然后输出完整代码，格式如下：

                ```%s
                完整可运行的代码（包含所有 import 和依赖）
                ```

                ⚠️ 规则：
                - 整个回复必须有且仅有一个 ``` 代码块
                - 代码块之外绝对不能出现任何代码片段
                - 代码块内必须包含完整可运行代码，不能省略、不能写"此处省略"
                """.formatted(language, language);
    }

    /** 工程项目生成的 System Prompt */
    public String getEngineeringProjectSystemPrompt() {
        return """
                你是软件架构师，根据需求生成完整工程项目。

                === 输出格式（严格遵守，否则解析失败） ===

                第一步：写1-3句项目概述（纯文本，不要用反引号）

                第二步：列出项目文件结构树（纯文本，不要用反引号），例如：
                project-name/
                ├── pom.xml
                ├── src/main/java/com/example/App.java
                └── README.md

                第三步：逐个输出文件代码。每个文件必须严格使用以下格式：

                ```语言标记
                // File: 文件在项目中的完整路径
                该文件的完整代码
                ```

                === 正确示例（请严格模仿） ===

                这是一个Spring Boot计算器项目，实现了加减乘除功能。

                calculator/
                ├── pom.xml
                ├── src/main/java/com/example/Calculator.java
                └── src/main/java/com/example/Main.java

                项目配置文件：

                ```xml
                // File: pom.xml
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>calculator</artifactId>
                  <version>1.0.0</version>
                </project>
                ```

                核心计算类：

                ```java
                // File: src/main/java/com/example/Calculator.java
                package com.example;
                public class Calculator {
                    public int add(int a, int b) { return a + b; }
                    public int subtract(int a, int b) { return a - b; }
                }
                ```

                ⚠️ 严禁：
                - 严禁把目录树放进 ``` 代码块
                - 严禁创建空的代码块（如 ```java 文件名``` 中间没有代码）
                - 严禁在代码块标记同一行写文件名（错误例子：```java App.java）
                - 文件名必须写在代码块内部第一行，格式为 // File: 路径
                - 每个文件一个独立代码块，不能合并
                """;
    }

    /** 工程项目创建的 System Prompt — 极简标记格式 */
    public String getProjectSystemPrompt() {
        return """
                你是软件架构师。根据用户需求，创建完整的工程项目。

                === 输出格式（必须严格遵守，每个字符都要按此格式） ===

                [PROJECT] frontend（或 backend）
                [DESC] 项目的一句话描述

                然后逐个输出文件。每个文件必须严格按以下三行格式：

                [FILE] 文件路径
                ```语言标记
                该文件的完整代码
                ```

                === 正确示例（请一模一样地复制这个格式） ===

                [PROJECT] frontend
                [DESC] Vue3数据表格组件，支持分页排序

                [FILE] package.json
                ```json
                {
                  "name": "vue3-table",
                  "version": "1.0.0",
                  "dependencies": {
                    "vue": "^3.4.0"
                  }
                }
                ```

                [FILE] src/App.vue
                ```vue
                <template>
                  <div id="app">
                    <DataTable :data="tableData" />
                  </div>
                </template>
                <script setup>
                import DataTable from './components/DataTable.vue'
                const tableData = [{ id: 1, name: 'Alice' }]
                </script>
                ```

                [FILE] src/components/DataTable.vue
                ```vue
                <template>
                  <table>
                    <tr v-for="row in data" :key="row.id">
                      <td>{{ row.name }}</td>
                    </tr>
                  </table>
                </template>
                <script setup>
                defineProps({ data: Array })
                </script>
                ```

                ⚠️ 致命错误（会导致解析失败）：
                - 不能用 [PROJECT_TYPE] 代替 [PROJECT]
                - 不能省略 [FILE] 标记
                - 不能把多个文件的代码合并在一个 ``` 块
                - [FILE] 下一行必须是 ```语言，再下一行开始写代码
                - 代码必须完整，不能写「此处省略」
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

    /** 构建消息列表（含多轮历史），工程模式下在用户消息前追加格式提醒 */
    public List<ChatMessage> buildMessages(String systemPrompt, String userPrompt,
                                            List<dev.langchain4j.data.message.ChatMessage> history, boolean isEngineering) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));

        if (history != null && !history.isEmpty()) {
            messages.addAll(history);
        }

        // 工程模式：在用户消息前追加格式标签，利用 recency bias 强化格式约束
        String finalPrompt = isEngineering
                ? "[FORMAT: 目录树用纯文本；每个文件一个 ```语言\\n// File: 路径\\n代码\\n``` 代码块；禁止空代码块]\n\n" + userPrompt
                : userPrompt;

        messages.add(UserMessage.from(finalPrompt));
        return messages;
    }
}
