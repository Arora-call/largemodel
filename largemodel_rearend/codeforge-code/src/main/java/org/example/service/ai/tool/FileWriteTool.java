/**
 * 模块：AI代码生成 - LangChain4j @Tool 工具
 * 功能：文件写入工具类，供 AI 模型在代码生成过程中调用
 * 作者：yx
 * 创建时间：2026-06-24
 */
package org.example.service.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.ProjectFile;
import org.example.mapper.ProjectFileMapper;
import org.example.service.ProjectService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 文件操作工具 — 供 LangChain4j AI 模型调用。
 * <p>
 * 提供文件写入、读取、删除、列表等能力，用于 Vue3 工程项目生成。
 * 每个工具方法和参数都添加了详细描述以减轻 AI 幻觉（错误调用或传参错误）。
 * <p>
 * 该类为请求作用域：每次代码生成请求创建一个新实例，
 * 绑定了当前对话 ID，确保不同对话的文件隔离。
 */
@Slf4j
public class FileWriteTool {

    private final Long conversationId;
    private final ProjectFileMapper projectFileMapper;
    private final ProjectService projectService;

    public FileWriteTool(Long conversationId, ProjectFileMapper projectFileMapper,
                         ProjectService projectService) {
        this.conversationId = conversationId;
        this.projectFileMapper = projectFileMapper;
        this.projectService = projectService;
    }

    /**
     * 创建或覆盖一个文件。用于在项目中写入源代码文件。
     * <p>
     * 这是 AI 模型最主要的文件操作入口。每个文件必须调用一次此工具。
     *
     * @param filePath 文件的相对路径，例如 "index.html"、"src/components/Header.vue"、"src/main.js"
     * @param content  文件的完整源代码内容，不能省略、不能写"此处省略"或"代码略"
     * @return 操作结果描述
     */
    @Tool("创建或覆盖一个源代码文件。用于在项目中写入文件。每个文件调用一次此工具。")
    public String writeFile(
            @P("文件的相对路径，例如 index.html、src/components/Header.vue、src/main.js")
            String filePath,
            @P("文件的完整源代码内容，必须是完整可运行的代码，不能省略任何部分")
            String content) {

        if (filePath == null || filePath.isBlank()) {
            return "错误: 文件路径不能为空";
        }
        if (content == null || content.isBlank()) {
            return "错误: 文件内容不能为空，请提供完整的源代码";
        }

        // 标准化路径（去掉开头的 /）
        String normalizedPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;

        // 检查是否已有同名文件，有则更新
        ProjectFile existing = projectFileMapper.findByConversationAndPath(conversationId, normalizedPath);
        if (existing != null) {
            existing.setContent(content);
            existing.setFileSize((long) content.length());
            projectFileMapper.updateById(existing);
            log.info("文件已更新: conversationId={}, path={}, size={}", conversationId, normalizedPath, content.length());
            return "文件 " + normalizedPath + " 已更新 (" + content.length() + " 字节)";
        }

        ProjectFile pf = ProjectFile.builder()
                .conversationId(conversationId)
                .filePath(normalizedPath)
                .content(content)
                .fileSize((long) content.length())
                .build();
        projectFileMapper.insert(pf);
        log.info("文件已创建: conversationId={}, path={}, size={}", conversationId, normalizedPath, content.length());
        return "文件 " + normalizedPath + " 已创建 (" + content.length() + " 字节)";
    }

    /**
     * 读取一个已有文件的内容。用于在修改代码前了解当前代码。
     *
     * @param filePath 要读取的文件相对路径
     * @return 文件内容，或文件不存在的提示
     */
    @Tool("读取一个已有文件的内容。用于在修改代码前了解当前代码。")
    public String readFile(
            @P("要读取的文件相对路径，例如 src/App.vue")
            String filePath) {

        if (filePath == null || filePath.isBlank()) {
            return "错误: 请指定要读取的文件路径";
        }

        String normalizedPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        ProjectFile pf = projectFileMapper.findByConversationAndPath(conversationId, normalizedPath);
        if (pf == null || pf.getContent() == null) {
            return "文件 " + normalizedPath + " 不存在，请先使用 writeFile 创建它";
        }
        return "文件 " + normalizedPath + ":\n```\n" + pf.getContent() + "\n```";
    }

    /**
     * 删除一个文件。
     *
     * @param filePath 要删除的文件相对路径
     * @return 操作结果
     */
    @Tool("删除一个不需要的文件。")
    public String deleteFile(
            @P("要删除的文件相对路径")
            String filePath) {

        if (filePath == null || filePath.isBlank()) {
            return "错误: 请指定要删除的文件路径";
        }

        String normalizedPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        ProjectFile pf = projectFileMapper.findByConversationAndPath(conversationId, normalizedPath);
        if (pf == null) {
            return "文件 " + normalizedPath + " 不存在，无需删除";
        }
        projectFileMapper.deleteById(pf.getId());
        log.info("文件已删除: conversationId={}, path={}", conversationId, normalizedPath);
        return "文件 " + normalizedPath + " 已删除";
    }

    /**
     * 列出当前项目中所有已创建的文件。
     *
     * @return 文件路径列表
     */
    @Tool("列出当前项目中所有已创建的文件，用于了解项目结构。")
    public String listFiles() {
        List<ProjectFile> files = projectFileMapper.findByConversationId(conversationId);
        if (files.isEmpty()) {
            return "当前项目还没有创建任何文件。请使用 writeFile 开始创建文件。";
        }
        return files.stream()
                .map(f -> "- " + f.getFilePath() + " (" + (f.getFileSize() != null ? f.getFileSize() : 0) + " 字节)")
                .collect(Collectors.joining("\n", "当前项目文件列表:\n", ""));
    }
}
