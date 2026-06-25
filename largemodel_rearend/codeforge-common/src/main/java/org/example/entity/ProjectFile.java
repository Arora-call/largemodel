/**
 * 模块：AI代码生成
 * 功能：项目文件实体，追踪 @Tool 工具创建的项目文件
 * 作者：yx
 * 创建时间：2026-06-24
 * 修改记录：
 *  2026-06-24 初始化 — 支持 LangChain4j @Tool 文件写入追踪
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
@Table(name = "project_files")
@TableName("project_files")
public class ProjectFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属对话 ID */
    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    /** 文件相对路径，如 src/components/Header.vue */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    /** 文件完整内容 */
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    /** 文件大小（字节） */
    @Column(name = "file_size")
    @Builder.Default
    private Long fileSize = 0L;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
