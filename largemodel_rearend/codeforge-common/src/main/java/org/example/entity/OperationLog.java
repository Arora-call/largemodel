/**
 * 操作日志
 */
package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@TableName("operation_logs")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String module;       // 模块: auth/code/admin
    private String action;       // 操作: CREATE/UPDATE/DELETE/LOGIN
    private String target;       // 操作对象: User(id=1)/Application(id=5)
    private Long operatorId;     // 操作人
    private String operatorName; // 操作人用户名
    private String detail;       // 详情
    private String ip;           // 请求 IP
    private Boolean success;     // 是否成功
    private LocalDateTime createdAt;
}
