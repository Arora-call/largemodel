/**
 * 模块：应用管理
 * 功能：应用实体，存储 AI 生成的应用元信息（代码文件存磁盘，不存 DB）
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 *  2026-06-25 新增 deployKey/initPrompt/priority/genStatus，sourceCode 改为废弃（改用磁盘存储）
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

    /** 代码生成类型: SINGLE_FILE / MULTI_FILE / VUE_PROJECT */
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String type = "SINGLE_FILE";

    /** 编程语言: html / vue / javascript 等 */
    @Column(length = 50)
    private String language;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 0-已删除, 1-草稿, 2-已生成 */
    @Column(nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    @Builder.Default
    private Integer status = 1;

    /** 生成状态: generating / completed / failed */
    @Column(name = "gen_status", length = 20)
    @Builder.Default
    private String genStatus = "completed";

    /** 创建应用时的初始 Prompt（便于复现和调试） */
    @Column(name = "init_prompt", columnDefinition = "TEXT")
    private String initPrompt;

    /** @deprecated 代码改为磁盘存储，此字段仅保留历史数据兼容 */
    @Deprecated
    @Column(name = "source_code", columnDefinition = "MEDIUMTEXT")
    private String sourceCode;

    @Column(name = "config_json", columnDefinition = "JSON")
    private String configJson;

    /** 封面图URL */
    @Column(name = "cover_image", length = 500)
    private String coverImage;

    /** 部署标识 — 6 位字母数字唯一 ID，作为访问路径 /<deployKey>/ */
    @Column(name = "deploy_key", length = 10, unique = true)
    private String deployKey;

    /** 部署时间 */
    @Column(name = "deployed_time")
    private LocalDateTime deployedTime;

    /** 优先级: 0-默认, 99-精选, 999-置顶 */
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer priority = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
