/**
 * Vue 项目构建器
 * 功能：对 AI 生成的 Vue3 工程执行 npm install + npm run build，产出 dist/ 目录
 * 作者：yx
 */
package org.example.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * 在 AI 生成 Vue 源码后，异步执行 npm 构建。
 * <p>
 * 构建流程：cd projectDir → npm install (5min) → npm run build (3min) → dist/
 * <p>
 * 使用 Java 21 虚拟线程，不阻塞 SSE 响应。
 */
@Slf4j
@Component
public class VueProjectBuilder {

    private static final int INSTALL_TIMEOUT_SEC = 300;
    private static final int BUILD_TIMEOUT_SEC = 180;
    private static final int WAIT_POLL_MS = 500;

    private final boolean isWindows = System.getProperty("os.name")
            .toLowerCase().contains("windows");

    /** 异步构建（虚拟线程，不阻塞调用方） */
    public void buildAsync(Path projectDir) {
        Thread.ofVirtual()
                .name("vue-builder-" + projectDir.getFileName())
                .start(() -> {
                    try {
                        boolean ok = build(projectDir);
                        log.info("Vue项目构建{}: path={}", ok ? "成功" : "失败", projectDir);
                    } catch (Exception e) {
                        log.error("Vue项目构建异常: path={}", projectDir, e);
                    }
                });
    }

    /** 同步构建，返回是否成功 */
    public boolean build(Path projectDir) {
        File dir = projectDir.toFile();
        if (!dir.exists() || !new File(dir, "package.json").exists()) {
            log.warn("Vue项目目录不存在或无 package.json: {}", dir.getAbsolutePath());
            return false;
        }

        log.info("开始构建Vue项目: path={}", dir.getAbsolutePath());

        // 1. npm install
        if (!runCommand(dir, npmCmd("install"), INSTALL_TIMEOUT_SEC)) {
            log.error("npm install 失败: path={}", dir.getAbsolutePath());
            return false;
        }

        // 2. npm run build
        if (!runCommand(dir, npmCmd("run", "build"), BUILD_TIMEOUT_SEC)) {
            log.error("npm run build 失败: path={}", dir.getAbsolutePath());
            return false;
        }

        // 3. 验证 dist 目录存在
        File distDir = new File(dir, "dist");
        if (!distDir.exists() || !new File(distDir, "index.html").exists()) {
            log.error("dist 目录未生成或缺少 index.html: {}", distDir.getAbsolutePath());
            return false;
        }

        log.info("Vue项目构建完成: path={}, dist={}", dir.getAbsolutePath(),
                distDir.getAbsolutePath());
        return true;
    }

    /** 等待构建完成（轮询 dist/index.html），最多 waitSeconds 秒 */
    public boolean waitForDist(Path projectDir, int waitSeconds) {
        File distIndex = projectDir.resolve("dist/index.html").toFile();
        long deadline = System.currentTimeMillis() + waitSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (distIndex.exists()) return true;
            try { Thread.sleep(WAIT_POLL_MS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
        }
        return false;
    }

    // ─── 内部方法 ───

    private boolean runCommand(File workDir, String[] cmd, int timeoutSec) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(workDir);
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            // 读取输出（避免进程阻塞）
            StringBuilder out = new StringBuilder();
            Thread.ofVirtual().start(() -> {
                try (var reader = new BufferedReader(
                        new InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        out.append(line).append('\n');
                    }
                } catch (Exception ignored) {}
            });

            boolean finished = proc.waitFor(timeoutSec, TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                log.warn("命令超时({}s): {} (workDir={})", timeoutSec,
                        String.join(" ", cmd), workDir.getName());
                return false;
            }

            int exit = proc.exitValue();
            if (exit != 0) {
                // 截取最后 500 字符日志
                String tail = out.length() > 500
                        ? out.substring(out.length() - 500) : out.toString();
                log.warn("命令退出码={}: {} (workDir={})\n{}",
                        exit, String.join(" ", cmd), workDir.getName(), tail.trim());
            }
            return exit == 0;
        } catch (Exception e) {
            log.error("命令执行异常: {} (workDir={})", String.join(" ", cmd),
                    workDir.getName(), e);
            return false;
        }
    }

    /** npm / npx 命令适配（Windows 加 .cmd） */
    private String[] npmCmd(String... args) {
        String npm = isWindows ? "npm.cmd" : "npm";
        String[] full = new String[args.length + 1];
        full[0] = npm;
        System.arraycopy(args, 0, full, 1, args.length);
        return full;
    }
}
