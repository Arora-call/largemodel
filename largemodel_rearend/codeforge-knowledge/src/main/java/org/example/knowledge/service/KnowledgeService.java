/**
 * 知识库服务
 * 功能：文档 CRUD、语义搜索（预留向量检索接口）
 * 作者：yx
 * 创建时间：2026-06-20
 */
package org.example.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.KnowledgeDocument;
import org.example.knowledge.mapper.KnowledgeDocumentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /** 上传文档 */
    @Transactional
    public KnowledgeDocument upload(MultipartFile file, Long userId, String collection) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String originalName = file.getOriginalFilename();
            String title = originalName != null ? originalName.replaceAll("\\.[^.]+$", "") : "未命名文档";
            String ext = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase() : "txt";

            KnowledgeDocument doc = KnowledgeDocument.builder()
                    .title(title)
                    .fileName(originalName)
                    .docType(ext)
                    .fileSize(file.getSize())
                    .content(content)
                    .summary(content.length() > 500 ? content.substring(0, 500) + "..." : content)
                    .collection(collection != null ? collection : "default")
                    .vectorStatus("pending")  // 待后续向量化
                    .userId(userId)
                    .build();

            docMapper.insert(doc);
            log.info("文档上传成功, id={}, title={}, size={}", doc.getId(), title, file.getSize());
            return doc;
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + e.getMessage(), e);
        }
    }

    /** 获取文档详情 */
    public KnowledgeDocument getById(Long id, Long userId) {
        KnowledgeDocument doc = docMapper.selectById(id);
        if (doc == null || !doc.getUserId().equals(userId)) {
            throw new RuntimeException("文档不存在或无权访问");
        }
        return doc;
    }

    /** 删除文档 */
    @Transactional
    public void delete(Long id, Long userId) {
        KnowledgeDocument doc = getById(id, userId);
        doc.setStatus(0);
        docMapper.updateById(doc);
        log.info("文档已删除, id={}", id);
    }

    /** 获取用户统计 */
    public Map<String, Object> getStats(Long userId) {
        long total = docMapper.countByUserId(userId);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalDocuments", total);
        return stats;
    }

    /** 语义搜索 — 预留接口，后续接入 Milvus/Chroma 向量检索 */
    public List<KnowledgeDocument> semanticSearch(Long userId, String query, int topK) {
        // 当前回退到关键词全文搜索
        return docMapper.search(Page.of(0, topK), userId, query).getRecords();
    }
}
