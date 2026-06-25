/**
 * 模块：应用管理
 * 功能：应用服务，处理应用保存/列表/详情/更新/删除/下载等业务
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.Application;
import org.example.entity.Conversation;
import org.example.entity.ProjectFile;
import org.example.exception.BusinessException;
import org.example.mapper.ProjectFileMapper;
import org.example.repository.ApplicationRepository;
import org.example.repository.ConversationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class ApplicationService {

    private final ApplicationRepository appRepo;
    private final ConversationRepository conversationRepo;
    private final ProjectFileMapper projectFileMapper;

    public ApplicationService(ApplicationRepository appRepo, ConversationRepository conversationRepo,
                              ProjectFileMapper projectFileMapper) {
        this.appRepo = appRepo;
        this.conversationRepo = conversationRepo;
        this.projectFileMapper = projectFileMapper;
    }

    /** 保存或更新应用 — 只保存元信息，代码通过对话链接获取 */
    @Transactional
    public Application saveOrUpdate(Long id, String name, String description, String type,
                                     String language, Long userId, Long conversationId) {
        Application app;
        if (id != null) {
            app = findOwnApp(id, userId);
        } else {
            app = Application.builder()
                    .userId(userId)
                    .status(2)  // 已生成
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        if (StringUtils.hasText(name)) app.setName(name);
        if (StringUtils.hasText(description)) app.setDescription(description);
        if (StringUtils.hasText(type)) app.setType(type);
        if (StringUtils.hasText(language)) app.setLanguage(language);
        if (app.getCoverImage() == null) {
            app.setCoverImage("https://picsum.photos/seed/" + app.hashCode() + "/400/300");
        }
        app.setUpdatedAt(LocalDateTime.now());
        app = appRepo.save(app);

        // 关联对话 → 应用
        if (conversationId != null) {
            linkConversation(app.getId(), conversationId);
        }

        return app;
    }

    /** 更新应用元信息（名称/描述/封面） */
    @Transactional
    public void updateMeta(Long id, Long userId, String name, String description, String coverImage) {
        Application app = findOwnApp(id, userId);
        if (StringUtils.hasText(name)) app.setName(name);
        if (description != null) app.setDescription(description);
        if (StringUtils.hasText(coverImage)) app.setCoverImage(coverImage);
        app.setUpdatedAt(LocalDateTime.now());
        appRepo.save(app);
    }

    /** 查找应用关联的对话ID */
    public Long findConversationIdByAppId(Long appId) {
        List<Conversation> convs = conversationRepo.findByApplicationIdOrderByUpdatedAtDesc(appId);
        return convs.isEmpty() ? null : convs.get(0).getId();
    }


    /** 分页列表 */
    public Page<Application> listByUser(Long userId, int page, int size, String keyword, String language, String type) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        // 组合筛选
        boolean hasKeyword = StringUtils.hasText(keyword);
        boolean hasLanguage = StringUtils.hasText(language);
        boolean hasType = StringUtils.hasText(type);

        if (hasKeyword && hasLanguage) {
            return appRepo.findByUserIdAndNameContainingAndLanguageAndStatusNot(
                    userId, keyword, language, 0, pageable);
        } else if (hasKeyword && hasType) {
            return appRepo.findByUserIdAndNameContainingAndTypeAndStatusNot(
                    userId, keyword, type, 0, pageable);
        } else if (hasKeyword) {
            return appRepo.findByUserIdAndNameContainingAndStatusNot(userId, keyword, 0, pageable);
        } else if (hasLanguage) {
            return appRepo.findByUserIdAndLanguageAndStatusNot(userId, language, 0, pageable);
        } else if (hasType) {
            return appRepo.findByUserIdAndTypeAndStatusNot(userId, type, 0, pageable);
        }
        return appRepo.findByUserIdAndStatusNotOrderByUpdatedAtDesc(userId, 0, pageable);
    }

    /** 详情 */
    public Application getById(Long id, Long userId) {
        return findOwnApp(id, userId);
    }

    /** 逻辑删除 */
    @Transactional
    public void delete(Long id, Long userId) {
        Application app = findOwnApp(id, userId);
        app.setStatus(0);
        app.setUpdatedAt(LocalDateTime.now());
        appRepo.save(app);
    }

    /** 关联对话到应用 */
    @Transactional
    public void linkConversation(Long appId, Long conversationId) {
        Conversation conv = conversationRepo.findById(conversationId).orElse(null);
        if (conv != null && !appId.equals(conv.getApplicationId())) {
            conv.setApplicationId(appId);
            conversationRepo.save(conv);
            log.info("对话关联到应用: conversationId={}, appId={}", conversationId, appId);
        }
    }

    /** 从 project_files 打包 ZIP（多文件独立，非拼接） */
    public byte[] zipFromProjectFiles(Long appId) {
        List<Conversation> convs = conversationRepo.findByApplicationIdOrderByUpdatedAtDesc(appId);
        for (Conversation conv : convs) {
            List<ProjectFile> pfs = projectFileMapper.findByConversationId(conv.getId());
            if (pfs.isEmpty()) continue;

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                 ZipOutputStream zos = new ZipOutputStream(bos)) {

                for (ProjectFile pf : pfs) {
                    if (pf.getContent() == null || pf.getContent().isBlank()) continue;
                    ZipEntry entry = new ZipEntry(pf.getFilePath());
                    zos.putNextEntry(entry);
                    zos.write(pf.getContent().getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                }
                zos.finish();
                log.info("ZIP从project_files打包: appId={}, 文件数={}", appId, pfs.size());
                return bos.toByteArray();
            } catch (IOException e) {
                log.warn("project_files ZIP打包失败: appId={}", appId, e);
                return null;
            }
        }
        return null;
    }

    private Application findOwnApp(Long id, Long userId) {
        Application app = appRepo.findById(id)
                .orElseThrow(() -> new BusinessException("应用不存在"));
        if (!app.getUserId().equals(userId)) throw new BusinessException("无权操作此应用");
        return app;
    }
}
