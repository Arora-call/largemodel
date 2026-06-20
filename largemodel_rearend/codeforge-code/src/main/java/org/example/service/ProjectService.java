/**
 * 模块：工程项目
 * 功能：项目文件系统服务 — 在磁盘上真实创建项目目录和文件
 * 作者：yx
 * 创建时间：2026-06-18
 */
package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.exception.BusinessException;
import org.example.util.LanguageUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
public class ProjectService {

    @Value("${app.upload.base-dir:uploads}")
    private String uploadBaseDir;

    /** 为项目创建真实目录和文件（所有文件放入 folderName 父文件夹），返回文件树 */
    public List<Map<String, Object>> createProjectFiles(Long projectId, String folderName, List<Map<String, String>> files) {
        Path projectDir = getProjectDir(projectId);
        try {
            // 清理旧目录
            if (Files.exists(projectDir)) deleteRecursive(projectDir);
            Files.createDirectories(projectDir);

            for (Map<String, String> file : files) {
                String path = file.get("path");
                String content = file.getOrDefault("content", "");
                // 所有文件放入 folderName/ 父文件夹下
                Path filePath = projectDir.resolve(folderName).resolve(path);
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, content);
                log.debug("创建文件: {}", filePath);
            }
            log.info("项目 {} 创建完成，共 {} 个文件，父文件夹: {}，目录: {}", projectId, files.size(), folderName, projectDir);
        } catch (IOException e) {
            log.error("创建项目文件失败: {}", e.getMessage());
            throw new BusinessException("项目文件创建失败: " + e.getMessage());
        }
        return getFileTree(projectId);
    }

    /** 获取项目目录树 */
    public List<Map<String, Object>> getFileTree(Long projectId) {
        Path projectDir = getProjectDir(projectId);
        if (!Files.exists(projectDir)) return List.of();
        List<Map<String, Object>> result = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(projectDir)) {
            stream.filter(Files::isRegularFile).forEach(f -> {
                String relativePath = projectDir.relativize(f).toString().replace('\\', '/');
                Map<String, Object> file = new HashMap<>();
                file.put("path", relativePath);
                file.put("language", LanguageUtil.detectByExt(relativePath));
                file.put("size", f.toFile().length());
                result.add(file);
            });
        } catch (IOException e) {
            log.error("读取项目目录失败: {}", e.getMessage());
        }
        result.sort(Comparator.comparing(m -> (String) m.get("path")));
        return result;
    }

    /** 读取单个文件内容 */
    public String readFile(Long projectId, String filePath) {
        try {
            Path path = getProjectDir(projectId).resolve(filePath).normalize();
            if (!path.startsWith(getProjectDir(projectId))) throw new BusinessException("非法文件路径");
            if (!Files.exists(path)) throw new BusinessException("文件不存在: " + filePath);
            return Files.readString(path);
        } catch (IOException e) {
            throw new BusinessException("读取文件失败: " + e.getMessage());
        }
    }

    /** 下载项目 ZIP */
    public Path createZip(Long projectId) {
        Path projectDir = getProjectDir(projectId);
        Path zipPath = Path.of(uploadBaseDir, "projects", "project_" + projectId + ".zip");
        try {
            Files.createDirectories(zipPath.getParent());
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
                try (Stream<Path> stream = Files.walk(projectDir)) {
                    stream.filter(Files::isRegularFile).forEach(f -> {
                        try {
                            String entryName = projectDir.relativize(f).toString().replace('\\', '/');
                            zos.putNextEntry(new ZipEntry(entryName));
                            Files.copy(f, zos);
                            zos.closeEntry();
                        } catch (IOException ignored) {}
                    });
                }
            }
        } catch (IOException e) {
            throw new BusinessException("ZIP 打包失败: " + e.getMessage());
        }
        return zipPath;
    }

    private Path getProjectDir(Long projectId) {
        return Path.of(uploadBaseDir, "projects", "project_" + projectId);
    }

    private void deleteRecursive(Path dir) throws IOException {
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.delete(p); } catch (IOException ignored) {}
            });
        }
    }

    /** 批量更新项目文件（AI 修改后写回磁盘） */
    public List<Map<String, Object>> updateFiles(Long projectId, Map<String, String> filesMap) {
        Path projectDir = getProjectDir(projectId);
        try {
            for (Map.Entry<String, String> entry : filesMap.entrySet()) {
                String path = entry.getKey();
                String content = entry.getValue();
                if (path == null || path.isEmpty() || content == null) continue;
                Path filePath = projectDir.resolve(path).normalize();
                // 路径穿越保护
                if (!filePath.startsWith(projectDir)) {
                    log.warn("非法文件路径被拒绝: {}", path);
                    continue;
                }
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, content);
                log.debug("更新文件: {}", path);
            }
            log.info("项目 {} 更新 {} 个文件完成", projectId, filesMap.size());
        } catch (IOException e) {
            log.error("更新项目文件失败: {}", e.getMessage());
            throw new BusinessException("文件更新失败: " + e.getMessage());
        }
        return getFileTree(projectId);
    }

    /** 删除项目 — 清理磁盘文件 + ZIP */
    public void deleteProject(Long projectId) {
        Path projectDir = getProjectDir(projectId);
        Path zipPath = Path.of(uploadBaseDir, "projects", "project_" + projectId + ".zip");
        try {
            if (Files.exists(projectDir)) deleteRecursive(projectDir);
            if (Files.exists(zipPath)) Files.delete(zipPath);
            log.info("项目 {} 已删除", projectId);
        } catch (IOException e) {
            log.error("删除项目 {} 失败: {}", projectId, e.getMessage());
        }
    }

}
