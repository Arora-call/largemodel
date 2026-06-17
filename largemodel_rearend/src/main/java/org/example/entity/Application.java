/**
 * 模块：应用管理
 * 功能：应用实体，映射applications表，存储AI生成的代码应用
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** NATIVE: 原生应用, ENGINEERING: 工程项目 */
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String type = "NATIVE";

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 0-已删除, 1-草稿, 2-已生成 */
    @Column(nullable = false)
    @Builder.Default
    private Integer status = 1;

    @Column(name = "source_code", columnDefinition = "MEDIUMTEXT")
    private String sourceCode;

    @Column(name = "config_json", columnDefinition = "JSON")
    private String configJson;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
