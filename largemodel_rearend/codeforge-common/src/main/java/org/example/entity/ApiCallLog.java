/**
 * API 调用日志 — 用于监控统计
 */
package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@TableName("api_call_logs")
public class ApiCallLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String endpoint;     // 接口路径
    private Long userId;         // 调用用户
    private String modelName;    // 使用的模型
    private Integer tokenUsed;   // 消耗 Token
    private Long latencyMs;      // 响应延迟
    private Boolean success;     // 是否成功
    private String errorMsg;     // 错误信息
    private LocalDateTime createdAt;
}
