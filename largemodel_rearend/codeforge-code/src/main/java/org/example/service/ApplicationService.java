/**
 * 模块：应用管理
 * 功能：应用服务，处理应用保存/列表/详情/更新/删除/下载等业务
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.Application;
import org.example.exception.BusinessException;
import org.example.repository.ApplicationRepository;
import org.example.util.LanguageUtil;
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

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();
    private final ApplicationRepository appRepo;

    /** 保存或更新应用 */
    @Transactional
    public Application saveOrUpdate(Long id, String name, String description, String type,
                                     String language, Long userId, String sourceCode, String configJson) {
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
        if (sourceCode != null) app.setSourceCode(sourceCode);
        if (configJson != null && !configJson.isBlank()) {
            app.setConfigJson(configJson);
        } else if (sourceCode != null && (app.getConfigJson() == null || app.getConfigJson().isBlank())) {
            app.setConfigJson(buildConfig(sourceCode, language));
        }
        if (app.getCoverImage() == null) {
            app.setCoverImage(LanguageUtil.toCoverGradient(language));
        }
        app.setUpdatedAt(LocalDateTime.now());

        return appRepo.save(app);
    }


    /** 解析代码中的依赖和结构 */
    private String buildConfig(String code, String lang) {
        try {
            var config = new java.util.HashMap<String, Object>();
            var deps = new java.util.ArrayList<String>();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:import|require|from)\\s+['\"(]?([\\w./@-]+)").matcher(code);
            while (m.find()) deps.add(m.group(1));
            config.put("dependencies", deps.stream().distinct().limit(10).toList());

            var files = new java.util.ArrayList<String>();
            java.util.regex.Matcher fm = java.util.regex.Pattern.compile(">>>\\s*FILE:\\s*(\\S+)").matcher(code);
            while (fm.find()) files.add(fm.group(1));
            config.put("files", files);
            config.put("language", lang);

            return MAPPER.writeValueAsString(config);
        } catch (Exception e) { return "{}"; }
    }

    /** 分页列表 */
    public Page<Application> listByUser(Long userId, int page, int size, String keyword, String language) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        if (StringUtils.hasText(keyword) && StringUtils.hasText(language)) {
            return appRepo.findByUserIdAndNameContainingAndLanguageAndStatusNot(
                    userId, keyword, language, 0, pageable);
        } else if (StringUtils.hasText(keyword)) {
            return appRepo.findByUserIdAndNameContainingAndStatusNot(userId, keyword, 0, pageable);
        } else if (StringUtils.hasText(language)) {
            return appRepo.findByUserIdAndLanguageAndStatusNot(userId, language, 0, pageable);
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

    /** 生成下载内容 */
    public byte[] downloadCode(Long id, Long userId) {
        Application app = findOwnApp(id, userId);
        String code = app.getSourceCode();
        if (code == null || code.isBlank()) {
            throw new BusinessException("应用无源代码可下载");
        }

        String lang = app.getLanguage();
        if ("native".equalsIgnoreCase(lang) || lang == null || "java".equalsIgnoreCase(lang)
                || "python".equalsIgnoreCase(lang)) {
            // 单文件直接返回
            String fileName = (app.getName() != null ? app.getName() : "app") + LanguageUtil.toExt(lang);
            return wrapSingleFile(fileName, code);
        }
        // 多文件用 ZIP
        return wrapZip(app.getName(), code);
    }

    private byte[] wrapSingleFile(String fileName, String code) {
        return code.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] wrapZip(String appName, String code) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(bos)) {
            String[] parts = code.split(">>>\\s*FILE:\\s*");
            if (parts.length <= 1) {
                ZipEntry entry = new ZipEntry("index.html");
                zos.putNextEntry(entry);
                zos.write(code.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            } else {
                for (int i = 1; i < parts.length; i++) {
                    String part = parts[i];
                    int nl = part.indexOf('\n');
                    String path = nl > 0 ? part.substring(0, nl).trim() : "file" + i;
                    String content = nl > 0 ? part.substring(nl + 1) : part;
                    ZipEntry entry = new ZipEntry(path);
                    zos.putNextEntry(entry);
                    zos.write(content.getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                }
            }
            zos.finish();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("打包失败: " + e.getMessage());
        }
    }

    private Application findOwnApp(Long id, Long userId) {
        Application app = appRepo.findById(id)
                .orElseThrow(() -> new BusinessException("应用不存在"));
        if (!app.getUserId().equals(userId)) throw new BusinessException("无权操作此应用");
        return app;
    }
}
