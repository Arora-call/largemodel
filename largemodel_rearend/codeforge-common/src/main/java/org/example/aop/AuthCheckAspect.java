/**
 * 模块：权限校验
 * 功能：AOP切面，拦截@AuthCheck注解，校验当前用户是否拥有所需角色
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.annotation.AuthCheck;
import org.example.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 权限校验切面
 * <p>
 * 执行流程：
 * <ol>
 *   <li>从 SecurityContext 获取当前认证信息</li>
 *   <li>检查是否已登录（未登录抛出 AccessDeniedException）</li>
 *   <li>检查 Principal 是否为 User 实体</li>
 *   <li>比对当前角色与 @AuthCheck 要求的角色</li>
 *   <li>角色不匹配则抛出 AccessDeniedException</li>
 *   <li>匹配则放行执行原方法</li>
 * </ol>
 */
@Aspect
@Component
public class AuthCheckAspect {

    private static final Logger log = LoggerFactory.getLogger(AuthCheckAspect.class);

    /**
     * 拦截所有带 @AuthCheck 注解的方法
     */
    @Around("@annotation(org.example.annotation.AuthCheck) || " +
            "@within(org.example.annotation.AuthCheck)")
    public Object checkAuth(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取注解信息
        AuthCheck authCheck = getAuthCheckAnnotation(joinPoint);
        String requiredRole = authCheck.value();

        // 2. 获取当前认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("未登录用户尝试访问: {}", joinPoint.getSignature());
            throw new AccessDeniedException("未登录，无法访问");
        }

        // 3. 获取当前用户实体
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User currentUser)) {
            log.warn("无效的认证主体类型: {}", principal != null ? principal.getClass() : "null");
            throw new AccessDeniedException("认证信息无效");
        }

        // 4. 角色校验
        String currentRole = currentUser.getRole().name();
        if (!requiredRole.equals(currentRole)) {
            log.warn("角色不匹配 — 用户: {}, 当前角色: {}, 需要角色: {}",
                    currentUser.getUsername(), currentRole, requiredRole);
            throw new AccessDeniedException(
                    String.format("权限不足，需要 %s 角色", requiredRole));
        }

        log.debug("权限校验通过 — 用户: {}, 角色: {}", currentUser.getUsername(), currentRole);
        return joinPoint.proceed();
    }

    /**
     * 从方法或类上提取 @AuthCheck 注解
     */
    private AuthCheck getAuthCheckAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 优先取方法级别，否则取类级别
        AuthCheck annotation = signature.getMethod().getAnnotation(AuthCheck.class);
        if (annotation != null) return annotation;
        return joinPoint.getTarget().getClass().getAnnotation(AuthCheck.class);
    }
}
