/**
 * 操作日志 AOP 切面 — 拦截 @LogRecord 注解自动记录操作
 */
package org.example.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.annotation.LogRecord;
import org.example.entity.OperationLog;
import org.example.entity.User;
import org.example.mapper.OperationLogMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class LogAspect {

    private final OperationLogMapper logMapper;
    private final HttpServletRequest request;

    @Around("@annotation(logRecord)")
    public Object around(ProceedingJoinPoint jp, LogRecord logRecord) throws Throwable {
        boolean success = true;
        String detail = "";
        try {
            Object result = jp.proceed();
            if (result != null) detail = result.toString().substring(0, Math.min(200, result.toString().length()));
            return result;
        } catch (Throwable e) {
            success = false;
            detail = e.getMessage();
            throw e;
        } finally {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String username = "anonymous";
                Long userId = null;
                if (auth != null && auth.getPrincipal() instanceof User user) {
                    username = user.getUsername();
                    userId = user.getId();
                }
                OperationLog ol = OperationLog.builder()
                        .module(logRecord.module()).action(logRecord.action()).target(logRecord.target())
                        .operatorId(userId).operatorName(username).detail(detail)
                        .ip(request.getRemoteAddr()).success(success)
                        .createdAt(LocalDateTime.now()).build();
                logMapper.insert(ol);
            } catch (Exception ignored) { /* 日志记录失败不影响业务 */ }
        }
    }
}
