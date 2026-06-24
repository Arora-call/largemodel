/**
 * CodeForge Knowledge Service — port 8083
 * 功能：RAG 知识库 — 文档上传、向量化、语义检索
 */
package org.example.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "org.example.knowledge", "org.example.mapper", "org.example.config" })
public class KnowledgeApplication {
    public static void main(String[] args) {
        System.setProperty("nacos.logging.default.config.enabled", "false");
        SpringApplication.run(KnowledgeApplication.class, args);
    }
}
