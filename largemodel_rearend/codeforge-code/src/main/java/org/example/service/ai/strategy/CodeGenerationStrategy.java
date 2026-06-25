/**
 * 模块：AI代码生成 - 策略模式
 * 功能：代码生成策略接口，每种生成模式对应一个策略实现
 * 作者：yx
 * 创建时间：2026-06-24
 */
package org.example.service.ai.strategy;

import org.example.enums.GenerateMode;
import org.example.dto.request.GenerateCodeRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 代码生成策略接口。
 * 每种生成模式（单文件/多文件/Vue3项目）对应一个独立的策略实现。
 */
public interface CodeGenerationStrategy {

    /** 返回该策略支持的生成模式 */
    GenerateMode supportedMode();

    /** 执行 SSE 流式代码生成 */
    SseEmitter generate(GenerateCodeRequest request, Long userId);
}
