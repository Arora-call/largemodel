/**
 * 模块：系统监控
 * 功能：健康检查控制器，提供服务状态探测接口
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.controller;

import org.example.dto.response.ApiResponse;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /** 图片代理 — 绕过 GFW/网络限制，从服务端获取外部图片 */
    @GetMapping("/proxy/image")
    public ResponseEntity<byte[]> proxyImage(@RequestParam String url) {
        // 安全：只允许 https 图片域名，防止 SSRF
        if (url == null || url.isBlank() || !url.startsWith("https://")) {
            return ResponseEntity.badRequest().build();
        }
        String host;
        try { host = new URI(url).getHost(); }
        catch (Exception e) { return ResponseEntity.badRequest().build(); }
        if (host == null) return ResponseEntity.badRequest().build();

        // 允许的图片域名白名单
        Set<String> allowed = Set.of(
            "picsum.photos", "fastly.picsum.photos", "i.picsum.photos",
            "images.unsplash.com", "plus.unsplash.com",
            "via.placeholder.com", "placehold.co", "placeholder.com",
            "dummyimage.com", "fakeimg.pl", "picsum.photos"
        );
        boolean trusted = allowed.contains(host) || host.endsWith(".picsum.photos");
        if (!trusted) {
            return ResponseEntity.status(403).build();
        }

        try {
            URL target = new URI(url).toURL();
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) target.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "CodeForge-ImageProxy/1.0");
            conn.setInstanceFollowRedirects(true);

            String contentType = conn.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                conn.disconnect();
                return ResponseEntity.status(415).build();
            }

            byte[] bytes;
            try (InputStream is = conn.getInputStream()) {
                bytes = is.readAllBytes();
            }
            conn.disconnect();

            // 缓存 1 小时
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(502).build();
        }
    }
}
