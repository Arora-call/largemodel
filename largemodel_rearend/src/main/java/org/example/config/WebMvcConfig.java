/**
 * 模块：系统基础设施
 * 功能：Web MVC 配置，映射静态资源路径（上传文件等）
 * 作者：yx
 * 创建时间：2026-06-18
 */
package org.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.base-dir:uploads}")
    private String uploadBaseDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射 /uploads/** 到文件系统 uploads/ 目录
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadBaseDir + "/");
    }
}
