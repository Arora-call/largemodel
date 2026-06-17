/**
 * 模块：用户体系
 * 功能：修改密码请求体，包含原密码和新密码字段
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.dto.request;

import lombok.Data;

@Data
public class UserPasswordRequest {
    private String oldPassword;
    private String newPassword;
}
