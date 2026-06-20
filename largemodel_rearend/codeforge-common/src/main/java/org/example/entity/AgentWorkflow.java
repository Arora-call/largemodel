/**
 * 模块：Agent 工作流
 * 功能：Agent 工作流实体，存储工作流定义与执行状态
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
@TableName("agent_workflows")
public class AgentWorkflow {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工作流名称 */
    private String name;

    /** 工作流描述 */
    private String description;

    /** Agent 链: 逗号分隔的 agent 列表 */
    private String agentChain;

    /** 状态: PENDING / RUNNING / COMPLETED / FAILED */
    @Builder.Default
    private String status = "PENDING";

    /** 执行结果汇总 (JSON) */
    private String result;

    /** 用户需求（原始输入） */
    private String requirement;

    /** 所属用户 */
    private Long userId;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
