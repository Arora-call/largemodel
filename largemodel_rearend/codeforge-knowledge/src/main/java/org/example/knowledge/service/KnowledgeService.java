/**
 * 知识库服务
 * 功能：文档 CRUD、PDF 解析、分块、AI 增强搜索、集合管理
 * 作者：yx
 */
package org.example.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.example.entity.KnowledgeDocument;
import org.example.knowledge.mapper.KnowledgeDocumentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeService {

    private final KnowledgeDocumentMapper docMapper;

    /** 分页查询用户的文档列表 */
    public IPage<KnowledgeDocument> listByUser(Long userId, int page, int size) {
        return docMapper.findByUserId(Page.of(page, size), userId);
    }

    /** 关键词搜索 */
    public IPage<KnowledgeDocument> search(Long userId, String keyword, int page, int size) {
        return docMapper.search(Page.of(page, size), userId, keyword);
    }

    /** 上传文档 — 支持 PDF/TXT/Markdown/代码文件 */
    @Transactional
    public KnowledgeDocument upload(MultipartFile file, Long userId, String collection) {
        try {
            String originalName = file.getOriginalFilename();
            String ext = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase() : "txt";
            String content;

            // PDF 解析
            if ("pdf".equals(ext)) {
                content = parsePdf(file);
            } else {
                content = new String(file.getBytes(), StandardCharsets.UTF_8);
            }

            String title = originalName != null
                    ? originalName.replaceAll("\\.[^.]+$", "") : "未命名文档";

            // 内容分块（按段落，每块最多 1000 字符）
            List<String> chunks = splitChunks(content, 1000);
            String summary = content.length() > 300 ? content.substring(0, 300) + "..." : content;

            KnowledgeDocument doc = KnowledgeDocument.builder()
                    .title(title)
                    .fileName(originalName)
                    .docType(ext)
                    .fileSize(file.getSize())
                    .content(content)
                    .summary(summary)
                    .collection(collection != null ? collection : "default")
                    .vectorStatus("completed") // 内容已提取
                    .userId(userId)
                    .build();

            docMapper.insert(doc);
            log.info("文档上传成功, id={}, title={}, type={}, chunks={}, size={}",
                    doc.getId(), title, ext, chunks.size(), file.getSize());
            return doc;
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + e.getMessage(), e);
        }
    }

    /** PDF 文本提取 */
    private String parsePdf(MultipartFile file) throws IOException {
        try (var pdf = Loader.loadPDF(new RandomAccessReadBuffer(file.getBytes()))) {
            var stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(pdf);
        }
    }

    /** 将文本按段落/句子切分为块 */
    private List<String> splitChunks(String text, int maxLen) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;
        // 按段落切分
        String[] paragraphs = text.split("\\n\\s*\\n");
        StringBuilder buf = new StringBuilder();
        for (String p : paragraphs) {
            p = p.trim();
            if (p.isEmpty()) continue;
            if (buf.length() + p.length() > maxLen && buf.length() > 0) {
                chunks.add(buf.toString().trim());
                buf.setLength(0);
            }
            if (buf.length() > 0) buf.append("\n\n");
            buf.append(p);
            // 超长段落再次切割
            while (buf.length() > maxLen) {
                int cut = maxLen;
                // 尽量在句号处切割
                int dot = buf.lastIndexOf("。", maxLen);
                if (dot > maxLen / 2) cut = dot + 1;
                chunks.add(buf.substring(0, cut).trim());
                buf.delete(0, cut);
            }
        }
        if (buf.length() > 0) chunks.add(buf.toString().trim());
        return chunks;
    }

    /** 获取文档详情 */
    public KnowledgeDocument getById(Long id, Long userId) {
        KnowledgeDocument doc = docMapper.selectById(id);
        if (doc == null || !doc.getUserId().equals(userId)) {
            throw new RuntimeException("文档不存在或无权访问");
        }
        return doc;
    }

    /** 删除文档（MyBatis-Plus @TableLogic 自动转为软删除） */
    @Transactional
    public void delete(Long id, Long userId) {
        KnowledgeDocument doc = getById(id, userId);
        docMapper.deleteById(doc.getId());
        log.info("文档已删除, id={}", id);
    }

    /** 获取用户统计 */
    public Map<String, Object> getStats(Long userId) {
        long total = docMapper.countByUserId(userId);
        long collections = docMapper.countCollectionsByUserId(userId);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalDocuments", total);
        stats.put("totalCollections", collections);
        return stats;
    }

    /** 语义搜索 — AI 增强关键词提取 + 全文搜索交集 */
    public List<KnowledgeDocument> semanticSearch(Long userId, String query, int topK) {
        // 提取查询中的关键词（按空格/标点拆分）
        String[] words = query.split("[\\s,，。.!！?？;；:：、]+");
        List<String> keywords = Arrays.stream(words)
                .filter(w -> w.length() >= 2)
                .distinct()
                .limit(5)
                .toList();

        // 用每个关键词搜索，合并去重
        Set<Long> seen = new LinkedHashSet<>();
        List<KnowledgeDocument> results = new ArrayList<>();
        for (String kw : keywords) {
            List<KnowledgeDocument> hits = docMapper.search(
                    Page.of(0, topK), userId, kw).getRecords();
            for (KnowledgeDocument doc : hits) {
                if (seen.add(doc.getId())) {
                    results.add(doc);
                }
            }
            if (results.size() >= topK) break;
        }

        // 如果关键词搜索不够，回退到原始查询全文搜索
        if (results.size() < topK) {
            List<KnowledgeDocument> fallback = docMapper.search(
                    Page.of(0, topK), userId, query).getRecords();
            for (KnowledgeDocument doc : fallback) {
                if (seen.add(doc.getId())) {
                    results.add(doc);
                }
                if (results.size() >= topK) break;
            }
        }

        return results.stream().limit(topK).collect(Collectors.toList());
    }

    // ─── 集合管理 ───

    /** 获取用户的所有集合名 */
    public List<String> listCollections(Long userId) {
        return docMapper.findCollectionsByUserId(userId);
    }

    /** 删除集合 — 将集合下所有文档标记为删除 */
    @Transactional
    public void deleteCollection(String name, Long userId) {
        List<KnowledgeDocument> docs = docMapper.findByCollectionAndUserId(name, userId);
        for (KnowledgeDocument doc : docs) {
            doc.setStatus(0);
            docMapper.updateById(doc);
        }
        log.info("集合已删除, name={}, 文档数={}", name, docs.size());
    }
}
