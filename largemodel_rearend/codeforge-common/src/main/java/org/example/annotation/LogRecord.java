/**
 * 操作日志记录注解 — 标注在 Controller 方法上自动记录
 */
package org.example.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogRecord {
    String module() default "admin";
    String action();
    String target() default "";
}
