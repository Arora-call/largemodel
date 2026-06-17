/**
 * 模块：AI可视化编辑
 * 功能：代码修改请求体，携带当前代码、选中元素信息和修改要求
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.dto.request;

import lombok.Data;

@Data
public class CodeModifyRequest {

    /** 当前完整代码 */
    private String currentCode;

    /** 选中的元素描述（tag/class/text/位置等） */
    private String elementInfo;

    /** 用户的修改要求 */
    private String modifyPrompt;

    /** 对话ID（可选，关联历史） */
    private Long conversationId;
}
