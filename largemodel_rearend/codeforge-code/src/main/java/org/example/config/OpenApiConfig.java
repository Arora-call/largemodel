/**
 * 模块：基础设施
 * 功能：Swagger / OpenAPI 配置，生成 API 文档页面
 * 作者：yx
 * 创建时间：2026-06-18
 */
package org.example.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("大模型应用平台 API")
                        .version("1.0.0")
                        .description("大模型代码应用生成平台后端接口文档，支持 AI 代码生成、用户管理、应用管理等功能")
                        .contact(new Contact()
                                .name("yx")));
    }
}
