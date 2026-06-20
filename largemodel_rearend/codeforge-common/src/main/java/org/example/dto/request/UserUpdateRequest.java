/**
 * 模块：用户体系
 * 功能：用户信息更新请求体，包含昵称、邮箱、手机号、头像等可修改字段
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.dto.request;

import lombok.Data;

@Data
public class UserUpdateRequest {

    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Integer status;
}
