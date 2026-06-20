/**
 * 模块：知识库
 * 功能：知识库文档实体，存储上传的文档元信息与内容
 * 作者：yx
 * 创建时间：2026-06-20
 */
package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("knowledge_documents")
public class KnowledgeDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文档标题 */
    private String title;

    /** 文档类型: pdf / markdown / txt / code */
    private String docType;

    /** 原始文件名 */
    private String fileName;

    /** 文件大小 (bytes) */
    private Long fileSize;

    /** 文档原始内容 (MEDIUMTEXT) */
    private String content;

    /** 摘要 (前500字) */
    private String summary;

    /** 所属知识库集合 */
    @Builder.Default
    private String collection = "default";

    /** 向量化状态: pending / completed / failed */
    @Builder.Default
    private String vectorStatus = "pending";

    /** 上传用户ID */
    private Long userId;

    /** 状态: 0-已删除, 1-正常 (MyBatis-Plus 逻辑删除) */
    @Builder.Default
    @TableLogic(value = "1", delval = "0")
    private Integer status = 1;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
