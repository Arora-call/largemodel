/**
 * 模块：应用部署
 * 功能：应用部署服务 — deployKey 生成、磁盘文件写入/清理、Nginx 部署目录管理
 * 作者：yx
 * 创建时间：2026-06-25
 */
package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.Application;
import org.example.entity.Conversation;
import org.example.entity.ProjectFile;
import org.example.mapper.ProjectFileMapper;
import org.example.repository.ApplicationRepository;
import org.example.repository.ConversationRepository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 部署服务。
 * <p>
 * 文件存储路径：
 * <pre>
 *   tmp/code_output/<type>_<appId>/    ← AI 生成的文件（开发/预览）
 *   tmp/code_deploy/<deployKey>/       ← 部署后的文件（Nginx 提供访问）
 * </pre>
 */
@Slf4j
@Service
public class DeployService {

    private static final String OUTPUT_ROOT = System.getProperty("user.dir") + "/tmp/code_output";
    private static final String DEPLOY_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int DEPLOY_KEY_LEN = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApplicationRepository appRepo;
    private final ProjectFileMapper projectFileMapper;
    private final ConversationRepository conversationRepo;
    private final org.example.service.ai.VueProjectBuilder vueProjectBuilder;

    /** 部署根目录（在 application.yml 中配置：codegen.deploy.root=${user.dir}/tmp/code_deploy） */
    @org.springframework.beans.factory.annotation.Value("${codegen.deploy.root}")
    private String deployRoot;

    public DeployService(ApplicationRepository appRepo, ProjectFileMapper projectFileMapper,
                         ConversationRepository conversationRepo,
                         org.example.service.ai.VueProjectBuilder vueProjectBuilder) {
        this.appRepo = appRepo;
        this.projectFileMapper = projectFileMapper;
        this.conversationRepo = conversationRepo;
        this.vueProjectBuilder = vueProjectBuilder;
    }

    @jakarta.annotation.PostConstruct
    void init() {
        // 确保部署根目录存在
        File dir = new File(deployRoot);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            log.info("部署根目录创建: path={}, created={}", dir.getAbsolutePath(), created);
        }
    }

    /** 将文件写入磁盘 */
    public File writeFilesToDisk(Long conversationId, Long appId, String genType,
                                  List<Map<String, String>> files) {
        String dirName = genType.toLowerCase() + "_" + (appId != null ? appId : "conv" + conversationId);
        File dir = new File(OUTPUT_ROOT, dirName);
        if (!dir.exists()) dir.mkdirs();

        for (Map<String, String> f : files) {
            String path = f.get("path");
            String content = f.get("content");
            if (content == null) continue;
            File target = new File(dir, path);
            target.getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(target)) {
                w.write(content);
            } catch (IOException e) {
                log.warn("磁盘写入失败: {}", target.getAbsolutePath(), e);
            }
        }
        log.info("文件写入磁盘: dir={}, 文件数={}", dir.getAbsolutePath(), files.size());
        return dir;
    }

    /** 级联清理：删除 project_files 表 + 磁盘文件 */
    public void cleanupConversation(Long conversationId) {
        // 1. 删除 project_files 表记录
        List<ProjectFile> pfs = projectFileMapper.findByConversationId(conversationId);
        for (ProjectFile pf : pfs) {
            projectFileMapper.deleteById(pf.getId());
        }
        // 2. 删除磁盘文件
        String dirName = "conv" + conversationId;
        deleteDir(new File(OUTPUT_ROOT, dirName));
        // 也尝试按 appId 命名的目录
        for (String type : List.of("single_file", "multi_file", "vue_project")) {
            deleteDir(new File(OUTPUT_ROOT, type + "_conv" + conversationId));
        }
        log.info("对话清理完成: conversationId={}, 删除了{}个文件记录", conversationId, pfs.size());
    }

    /** 级联清理应用及其部署文件 */
    public void cleanupApplication(Long appId, Application app) {
        // 删除磁盘上的生成目录
        if (app != null && app.getType() != null) {
            deleteDir(new File(OUTPUT_ROOT, app.getType().toLowerCase() + "_" + appId));
        }
        // 删除部署目录
        if (app != null && app.getDeployKey() != null) {
            deleteDir(new File(deployRoot, app.getDeployKey()));
        }
        log.info("应用清理完成: appId={}", appId);
    }

    /** 部署应用：复制文件到 Nginx 部署目录并生成 deployKey */
    public String deploy(Long appId, Application app, List<Map<String, String>> files) {
        // 已有 deployKey 则复用
        String deployKey = app.getDeployKey();
        if (deployKey == null || deployKey.isBlank()) {
            deployKey = generateDeployKey();
        }

        // 确保部署根目录存在
        File rootDir = new File(deployRoot);
        if (!rootDir.exists()) {
            rootDir.mkdirs();
            log.info("部署根目录已创建: {}", rootDir.getAbsolutePath());
        }

        // 复制文件到部署目录
        File deployDir = new File(deployRoot, deployKey);
        deleteDir(deployDir); // 先清旧文件
        boolean created = deployDir.mkdirs();
        log.info("部署目录操作: path={}, created={}, deployRoot={}, deployKey={}",
                deployDir.getAbsolutePath(), created, deployRoot, deployKey);

        int writtenCount = 0;
        for (Map<String, String> f : files) {
            String path = f.get("path");
            String content = f.get("content");
            if (content == null) continue;
            File target = new File(deployDir, path);
            target.getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(target)) {
                w.write(content);
                writtenCount++;
            } catch (IOException e) {
                log.warn("部署文件写入失败: {}", target.getAbsolutePath(), e);
            }
        }

        // 更新数据库
        app.setDeployKey(deployKey);
        app.setDeployedTime(LocalDateTime.now());
        appRepo.save(app);

        log.info("部署完成: appId={}, deployKey={}, deployDir={}, 写入文件数={}, 目录内容={}",
                appId, deployKey, deployDir.getAbsolutePath(), writtenCount,
                deployDir.exists() ? java.util.Arrays.toString(deployDir.list()) : "目录不存在");
        return deployKey;
    }

    /**
     * 从对话部署 — 读取 project_files 写入 Nginx 部署目录。
     * 自动关联或创建 Application 来管理 deployKey。
     *
     * @return [deployKey, url] 的 Map
     */
    public Map<String, String> deployConversation(Long conversationId) {
        // 1. 读取对话关联的项目文件
        List<ProjectFile> pfs = projectFileMapper.findByConversationId(conversationId);
        if (pfs.isEmpty()) {
            throw new IllegalStateException("该对话没有可部署的文件，请先生成代码");
        }

        // 2. 转为文件列表
        List<Map<String, String>> files = new ArrayList<>();
        for (ProjectFile pf : pfs) {
            if (pf.getContent() != null && !pf.getContent().isBlank()) {
                files.add(Map.of("path", pf.getFilePath(), "content", pf.getContent()));
            }
        }
        if (files.isEmpty()) {
            throw new IllegalStateException("该对话没有有效的文件内容");
        }

        // 3. 部署到 Nginx 目录（不自动创建 Application，需用户手动保存）
        Conversation conv = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("对话不存在: " + conversationId));

        String deployKey = generateDeployKey();
        File deployDir = new File(deployRoot, deployKey);
        deleteDir(deployDir);
        deployDir.mkdirs();

        // Vue 项目：等待 npm build 完成后，复制 dist/ 目录
        boolean isVueProject = "VUE_PROJECT".equalsIgnoreCase(conv.getType());
        if (isVueProject) {
            java.nio.file.Path srcDir = java.nio.file.Path.of(
                    System.getProperty("user.dir"),
                    "tmp/code_output/vue_project_conv" + conversationId);
            // 等待构建完成（最多 120s）
            boolean built = vueProjectBuilder.waitForDist(srcDir, 120);
            if (built) {
                try {
                    copyDir(new File(srcDir.toFile(), "dist"), deployDir);
                        // 注入元素选取脚本到 HTML 文件
                        injectPickerIntoDeployDir(deployDir);
                    log.info("Vue项目dist部署完成: {} → {}", srcDir.resolve("dist"),
                            deployDir.getAbsolutePath());
                } catch (IOException e) {
                    log.error("Vue项目dist复制失败", e);
                    // 回退：写源码文件
                    writeFilesToDeployDir(files, deployDir);
                }
            } else {
                log.warn("Vue项目构建未完成，回退到源码部署");
                writeFilesToDeployDir(files, deployDir);
            }
        } else {
            writeFilesToDeployDir(files, deployDir);
        }

        // 4. 如果已有 Application，更新 deployKey/deployedTime（不更新 sourceCode）
        if (conv.getApplicationId() != null) {
            appRepo.findById(conv.getApplicationId()).ifPresent(app -> {
                app.setDeployKey(deployKey);
                app.setDeployedTime(LocalDateTime.now());
                appRepo.save(app);
            });
        }

        // 5. 返回访问信息
        String url = "http://localhost/" + deployKey + "/";
        log.info("对话部署完成: conversationId={}, deployKey={}, url={}, 文件数={}",
                conversationId, deployKey, url, files.size());
        return Map.of("deployKey", deployKey, "url", url);
    }

    /**
     * 从应用 ID 部署 — 优先查找关联对话的 project_files，否则从 sourceCode 解析。
     */
    public Map<String, String> deployByAppId(Long appId) {
        log.info("应用部署请求: appId={}", appId);
        Application app = appRepo.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("应用不存在: " + appId));
        log.info("应用信息: name={}, type={}, sourceCode长度={}",
                app.getName(), app.getType(),
                app.getSourceCode() != null ? app.getSourceCode().length() : 0);

        // 1. 尝试从关联对话的 project_files 部署
        List<Conversation> convs = conversationRepo.findByApplicationIdOrderByUpdatedAtDesc(appId);
        log.info("关联对话数: {}", convs.size());
        for (Conversation conv : convs) {
            List<ProjectFile> pfs = projectFileMapper.findByConversationId(conv.getId());
            log.info("  对话 {}: project_files数={}, type={}", conv.getId(), pfs.size(), conv.getType());
            if (!pfs.isEmpty()) {
                String deployKey = app.getDeployKey() != null && !app.getDeployKey().isBlank()
                        ? app.getDeployKey() : generateDeployKey();
                File deployDir = new File(deployRoot, deployKey);
                deleteDir(deployDir);
                deployDir.mkdirs();

                // Vue 项目：等待 dist/ 构建完成，复制构建产物
                boolean isVue = "VUE_PROJECT".equalsIgnoreCase(conv.getType());
                if (isVue) {
                    java.nio.file.Path srcDir = java.nio.file.Path.of(
                            System.getProperty("user.dir"),
                            "tmp/code_output/vue_project_conv" + conv.getId());
                    boolean built = vueProjectBuilder.waitForDist(srcDir, 120);
                    if (built) {
                        try {
                            copyDir(new File(srcDir.toFile(), "dist"), deployDir);
                            injectPickerIntoDeployDir(deployDir);
                            log.info("Vue项目dist部署: appId={}, deployKey={}", appId, deployKey);
                        } catch (IOException e) {
                            log.error("Vue dist复制失败", e);
                            throw new IllegalStateException("Vue 项目构建产物复制失败");
                        }
                    } else {
                        throw new IllegalStateException("Vue 项目构建未完成，请稍后重试部署");
                    }
                } else {
                    // 非 Vue：写源码文件
                    List<Map<String, String>> files = new ArrayList<>();
                    for (ProjectFile pf : pfs) {
                        if (pf.getContent() != null && !pf.getContent().isBlank()) {
                            files.add(Map.of("path", pf.getFilePath(), "content", pf.getContent()));
                        }
                    }
                    writeFilesToDeployDir(files, deployDir);
                }

                // 更新数据库
                app.setDeployKey(deployKey);
                app.setDeployedTime(LocalDateTime.now());
                appRepo.save(app);

                String url = "http://localhost/" + deployKey + "/";
                log.info("应用部署完成: appId={}, deployKey={}, url={}, vue={}",
                        appId, deployKey, url, isVue);
                return Map.of("deployKey", deployKey, "url", url);
            }
        }

        // 2. 回退：从 sourceCode 解析文件
        String sourceCode = app.getSourceCode();
        if (sourceCode == null || sourceCode.isBlank()) {
            throw new IllegalStateException("该应用没有可部署的代码，sourceCode 为空");
        }

        log.info("尝试从 sourceCode 解析文件, 长度={}", sourceCode.length());
        List<Map<String, String>> files = parseConcatenatedCodePublic(sourceCode);
        log.info("sourceCode 解析出文件数: {}", files.size());
        if (files.isEmpty()) {
            throw new IllegalStateException("无法从应用代码中解析出文件");
        }

        String deployKey = deploy(appId, app, files);
        String url = "http://localhost/" + deployKey + "/";
        log.info("应用部署完成(sourceCode): appId={}, deployKey={}, 文件数={}", appId, deployKey, files.size());
        return Map.of("deployKey", deployKey, "url", url);
    }

    // 注释标记正则 — 支持 HTML/CSS/JS/通用 四种注释格式
    private static final java.util.regex.Pattern COMMENT_MARKER_RE = java.util.regex.Pattern.compile(
            "(?:<!--\\s*file:\\s*(.+?)\\s*-->|/\\*\\s*file:\\s*(.+?)\\s*\\*/|//\\s*file:\\s*(.+?)|#\\s*file:\\s*(.+?))\\n");

    // 旧格式兼容: // ===== path =====
    private static final java.util.regex.Pattern OLD_MARKER_RE = java.util.regex.Pattern.compile(
            "(?m)^// ===== (.+?) =====$");

    /** 公开的静态解析方法，供其他 Service 调用（如 ZIP 下载） */
    public static List<Map<String, String>> parseConcatenatedCodePublic(String code) {
        List<Map<String, String>> files = new ArrayList<>();
        java.util.regex.Matcher cm = COMMENT_MARKER_RE.matcher(code);
        int lastEnd = 0;
        String lastPath = null;
        while (cm.find()) {
            if (lastPath != null && cm.start() > lastEnd) {
                String content = code.substring(lastEnd, cm.start()).trim();
                if (!content.isEmpty()) {
                    files.add(Map.of("path", lastPath, "language", detectLangStatic(lastPath), "content", content));
                }
            }
            lastPath = cm.group(1) != null ? cm.group(1).trim()
                    : cm.group(2) != null ? cm.group(2).trim()
                    : cm.group(3) != null ? cm.group(3).trim()
                    : cm.group(4) != null ? cm.group(4).trim() : null;
            lastEnd = cm.end();
        }
        if (lastPath != null && lastEnd < code.length()) {
            String content = code.substring(lastEnd).trim();
            if (!content.isEmpty()) {
                files.add(Map.of("path", lastPath, "language", detectLangStatic(lastPath), "content", content));
            }
        }
        if (files.isEmpty()) {
            // 尝试旧格式
            java.util.regex.Matcher om = OLD_MARKER_RE.matcher(code);
            List<String> oldPaths = new ArrayList<>();
            List<Integer> oldStarts = new ArrayList<>();
            while (om.find()) {
                oldPaths.add(om.group(1).trim());
                oldStarts.add(om.end() + 1);
            }
            for (int i = 0; i < oldPaths.size(); i++) {
                String path = oldPaths.get(i);
                int start = oldStarts.get(i);
                int end = (i + 1 < oldStarts.size()) ? oldStarts.get(i + 1) - 1 : code.length();
                String content = code.substring(Math.min(start, code.length()),
                        Math.min(end, code.length())).trim();
                if (!path.isEmpty() && !content.isEmpty()) {
                    files.add(Map.of("path", path, "language", detectLangStatic(path), "content", content));
                }
            }
        }
        if (files.isEmpty()) {
            files.add(Map.of("path", "index.html", "language", "html", "content", code.trim()));
        }
        return files;
    }

    static String detectLangStatic(String path) {
        if (path == null) return "text";
        String lower = path.toLowerCase();
        if (lower.endsWith(".vue")) return "vue";
        if (lower.endsWith(".html")) return "html";
        if (lower.endsWith(".css")) return "css";
        if (lower.endsWith(".js")) return "javascript";
        if (lower.endsWith(".json")) return "json";
        return "text";
    }

    /** 生成 6 位不重复的 deployKey */
    private String generateDeployKey() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(DEPLOY_KEY_LEN);
            for (int i = 0; i < DEPLOY_KEY_LEN; i++) {
                sb.append(DEPLOY_CHARS.charAt(RANDOM.nextInt(DEPLOY_CHARS.length())));
            }
            String key = sb.toString();
            if (appRepo.findByDeployKey(key).isEmpty()) {
                return key;
            }
        }
        throw new IllegalStateException("无法生成唯一 deployKey，请稍后重试");
    }

    /** 将文件列表写入部署目录（非 Vue 项目用） */
    private void writeFilesToDeployDir(List<Map<String, String>> files, File deployDir) {
        for (Map<String, String> f : files) {
            String path = f.get("path");
            String content = f.get("content");
            if (content == null) continue;
            // HTML 文件注入元素选取脚本（用于跨域 iframe 预览场景）
            if (path != null && (path.endsWith(".html") || path.endsWith(".htm"))) {
                content = injectPickerScript(content);
            }
            File target = new File(deployDir, path);
            target.getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(target)) {
                w.write(content);
            } catch (IOException e) {
                log.warn("部署文件写入失败: {}", target.getAbsolutePath(), e);
            }
        }
    }

    /** 递归复制目录 */
    private void copyDir(File src, File dest) throws IOException {
        if (!src.exists()) return;
        dest.mkdirs();
        File[] children = src.listFiles();
        if (children == null) return;
        for (File child : children) {
            File target = new File(dest, child.getName());
            if (child.isDirectory()) {
                copyDir(child, target);
            } else {
                java.nio.file.Files.copy(child.toPath(), target.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /** 向 HTML 内容注入元素选取脚本（用于跨域 iframe 预览时支持元素选取） */
    private String injectPickerScript(String html) {
        String pickerJs = """
                <script>
                (function() {
                  var active = false, hovered = null, ov = null, style = null;
                  function enable() {
                    if (active) return; active = true;
                    ov = document.createElement("div");
                    ov.id = "__cf_picker_ov";
                    ov.style.cssText = "position:fixed;pointer-events:none;border:2px solid #7c8aff;background:rgba(124,138,255,.1);z-index:999999;display:none;border-radius:2px;";
                    document.head.appendChild(ov);
                    style = document.createElement("style");
                    style.id = "__cf_picker_style";
                    style.textContent = ".__cf_hover { outline: 2px solid #7c8aff !important; outline-offset: 2px; }";
                    document.head.appendChild(style);
                  }
                  function disable() {
                    active = false;
                    if (hovered) { hovered.classList.remove("__cf_hover"); hovered = null; }
                    if (ov) { ov.remove(); ov = null; }
                    if (style) { style.remove(); style = null; }
                  }
                  window.addEventListener("message", function(e) {
                    if (e.data && e.data.type === "cf-picker-activate") enable();
                    if (e.data && e.data.type === "cf-picker-deactivate") disable();
                  });
                  document.addEventListener("mouseover", function(e) {
                    if (!active) return;
                    if (hovered) hovered.classList.remove("__cf_hover");
                    var el = e.target;
                    if (!el || el === document.body || el === document.documentElement || el.id === "__cf_picker_ov") return;
                    el.classList.add("__cf_hover");
                    hovered = el;
                    var r = el.getBoundingClientRect();
                    ov.style.display = "block";
                    ov.style.top = r.top + "px";
                    ov.style.left = r.left + "px";
                    ov.style.width = r.width + "px";
                    ov.style.height = r.height + "px";
                  }, true);
                  document.addEventListener("mouseout", function(e) {
                    if (!active || hovered !== e.target) return;
                    if (hovered) { hovered.classList.remove("__cf_hover"); hovered = null; }
                    ov.style.display = "none";
                  }, true);
                  document.addEventListener("click", function(e) {
                    if (!active) return;
                    e.preventDefault();
                    e.stopPropagation();
                    var el = e.target;
                    if (!el || el.id === "__cf_picker_ov") return;
                    el.classList.remove("__cf_hover"); hovered = null;
                    var tag = el.tagName.toLowerCase();
                    var id = el.id || "";
                    var rawCls = el.className && typeof el.className === "string" ? String(el.className).trim() : "";
                    var classes = rawCls.replace(/\\b__cf_hover\\b/g, "").trim();
                    var text = (el.textContent || "").replace(/\\s+/g, " ").trim().substring(0, 60);
                    var selector = tag;
                    if (id) selector += "#" + id;
                    else if (classes) selector += "." + classes.split(/\\s+/).filter(Boolean).join(".");
                    window.parent.postMessage({ type: "cf-pick-element", data: { tag: tag, id: id, classes: classes, text: text, selector: selector, filePath: "index.html" } }, "*");
                  }, true);
                })();
                </script>""";

        if (html.contains("</body>")) {
            return html.replace("</body>", pickerJs + "\n</body>");
        }
        return html + "\n" + pickerJs;
    }

    /** 遍历部署目录，对所有 HTML 文件注入元素选取脚本 */
    private void injectPickerIntoDeployDir(File deployDir) {
        if (!deployDir.exists() || !deployDir.isDirectory()) return;
        File[] files = deployDir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                injectPickerIntoDeployDir(file);
            } else if (file.getName().endsWith(".html") || file.getName().endsWith(".htm")) {
                try {
                    String content = Files.readString(file.toPath());
                    String injected = injectPickerScript(content);
                    Files.writeString(file.toPath(), injected);
                    log.info("选取脚本注入: {}", file.getAbsolutePath());
                } catch (IOException e) {
                    log.warn("选取脚本注入失败: {}", file.getAbsolutePath(), e);
                }
            }
        }
    }

    private void deleteDir(File dir) {
        if (!dir.exists()) return;
        try {
            Files.walk(dir.toPath())
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            log.warn("目录删除失败: {}", dir.getAbsolutePath(), e);
        }
    }
}
