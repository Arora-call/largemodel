/**
 * 模块：AI 代码生成 — RAG 知识库上下文
 * 功能：根据用户选择或自动关键词检索，将知识库文档内容注入到 AI Prompt
 * 作者：yx
 * 创建时间：2026-06-25
 */
package org.example.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.KnowledgeDocument;
import org.example.mapper.KnowledgeDocMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库上下文服务。
 *
 * <p>每次 AI 调用前，将相关文档片段注入到 system prompt 中，
 * 使 AI 能够基于用户上传的资料生成更准确的代码。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeContextService {

    private final KnowledgeDocMapper knowledgeDocMapper;

    /** 单篇文档最大注入字符数（避免超出模型上下文窗口） */
    private static final int MAX_PER_DOC = 3000;
    /** 总上下文最大字符数 */
    private static final int MAX_TOTAL = 8000;

    /**
     * 根据用户手动选择的文档 ID 列表构建知识库上下文。
     *
     * @param docIds 文档 ID 列表
     * @return 格式化的知识库上下文字符串，无匹配则返回空字符串
     */
    public String buildContext(List<Long> docIds) {
        if (docIds == null || docIds.isEmpty()) return "";

        List<KnowledgeDocument> docs = knowledgeDocMapper.findByIds(docIds);
        if (docs.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n## 知识库参考文档\n");
        sb.append("以下是从你的知识库中检索到的相关文档内容，请在生成代码时参考：\n\n");

        int totalChars = 0;
        for (KnowledgeDocument doc : docs) {
            if (doc.getContent() == null || doc.getContent().isBlank()) continue;
            String snippet = doc.getContent();
            if (snippet.length() > MAX_PER_DOC) {
                snippet = snippet.substring(0, MAX_PER_DOC) + "\n...(内容已截断)";
            }
            sb.append("### 📄 ").append(doc.getTitle()).append("\n");
            sb.append("```\n").append(snippet).append("\n```\n\n");

            totalChars += snippet.length();
            if (totalChars > MAX_TOTAL) {
                sb.append("> ⚠️ 知识库内容已达上限，部分文档未包含。\n");
                break;
            }
        }

        log.info("知识库上下文构建完成: 文档数={}, 总字符={}", docs.size(), totalChars);
        return sb.toString();
    }

    /**
     * 根据用户 Prompt 自动检索相关文档并构建上下文。
     *
     * @param userId   当前用户 ID
     * @param prompt   用户输入的问题/需求
     * @param maxDocs  最多返回文档数
     * @return 格式化的上下文，无匹配则返回空字符串
     */
    public String buildContextByPrompt(Long userId, String prompt, int maxDocs) {
        if (prompt == null || prompt.isBlank()) return "";

        // 提取关键词（取 prompt 前 100 字符的关键部分）
        String keyword = extractKeywords(prompt);
        log.info("知识库自动检索: userId={}, keyword={}", userId, keyword);

        List<KnowledgeDocument> docs = knowledgeDocMapper.searchForAi(userId, keyword, maxDocs);
        if (docs.isEmpty()) {
            log.info("知识库自动检索无匹配结果");
            return "";
        }

        List<Long> ids = docs.stream().map(KnowledgeDocument::getId).toList();
        log.info("知识库自动检索匹配到 {} 篇文档: {}", docs.size(), ids);
        return buildContext(ids);
    }

    /** 从 Prompt 中提取关键词 */
    private String extractKeywords(String prompt) {
        // 截取前 100 字符，按空格/标点分割，取长度 >= 2 的词
        String head = prompt.length() > 100 ? prompt.substring(0, 100) : prompt;
        String[] words = head.split("[\\s,，。.!！?？;；:：、\\n\\r]+");
        List<String> meaningful = new ArrayList<>();
        for (String w : words) {
            if (w.length() >= 2 && w.length() <= 20) {
                meaningful.add(w);
            }
        }
        return meaningful.isEmpty() ? head : String.join(" ", meaningful);
    }
}
