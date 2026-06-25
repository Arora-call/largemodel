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

import java.util.List;
import java.util.Map;

@Data
public class CodeModifyRequest {

    /** 当前完整代码（拼接格式，兼容旧调用，新调用建议使用 files） */
    private String currentCode;

    /** 当前项目文件列表 [{path, language, content}] */
    private List<Map<String, String>> files;

    /** 选中的元素描述（tag/class/text/选择器等） */
    private String elementInfo;

    /** 用户的修改要求 */
    private String modifyPrompt;

    /** 对话ID（可选，关联历史） */
    private Long conversationId;

    /** 项目类型: SINGLE_FILE / MULTI_FILE / VUE_PROJECT */
    private String type;
}
