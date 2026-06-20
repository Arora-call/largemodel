/**
 * 模块：应用管理
 * 功能：应用实体，映射applications表，存储AI生成的代码应用
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "applications")
@TableName("applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** NATIVE: 原生应用, ENGINEERING: 工程项目 */
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String type = "NATIVE";

    /** 编程语言: java / vue / python / html 等 */
    @Column(length = 50)
    private String language;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 0-已删除, 1-草稿, 2-已生成 */
    @Column(nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    @Builder.Default
    private Integer status = 1;

    @Column(name = "source_code", columnDefinition = "MEDIUMTEXT")
    private String sourceCode;

    @Column(name = "config_json", columnDefinition = "JSON")
    private String configJson;

    /** 封面图URL */
    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
