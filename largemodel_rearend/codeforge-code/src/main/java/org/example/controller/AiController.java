/**
 * 模块：AI代码生成
 * 功能：AI控制器，提供 SSE 流式代码生成接口
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 *  2026-06-17 移除非流式生成，仅保留 SSE 流式
 */
package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.CodeModifyRequest;
import org.example.dto.request.GenerateCodeRequest;
import org.example.entity.User;
import org.example.service.ai.AiCodeGenService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiCodeGenService aiCodeGenService;

    /**
     * SSE 流式代码生成
     */
    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateStream(@Valid @RequestBody GenerateCodeRequest request,
                                      @AuthenticationPrincipal User user) {
        return aiCodeGenService.generateStream(request, user.getId());
    }

    /**
     * SSE 流式代码修改——基于现有代码 + 选中元素 + 修改需求
     */
    @PostMapping(value = "/modify/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter modifyStream(@RequestBody CodeModifyRequest request,
                                    @AuthenticationPrincipal User user) {
        return aiCodeGenService.modifyCodeStream(request, user.getId());
    }
}
