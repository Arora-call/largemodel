/**
 * 模块：权限校验
 * 功能：自定义权限校验注解，标记方法需要指定角色才能访问
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解
 * <p>
 * 用法：
 * <pre>{@code
 * @AuthCheck              // 默认需要 ADMIN 角色
 * @AuthCheck("ADMIN")     // 显式指定 ADMIN
 * @AuthCheck("USER")      // USER 即可
 * }</pre>
 * <p>
 * 底层通过 {@link org.example.aop.AuthCheckAspect} AOP 切面实现拦截
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * 需要的角色，默认 ADMIN
     */
    String value() default "ADMIN";
}
