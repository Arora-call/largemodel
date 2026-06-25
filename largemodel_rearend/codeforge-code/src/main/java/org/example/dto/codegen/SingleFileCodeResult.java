/**
 * 单文件模式 AI 结构化输出
 */
package org.example.dto.codegen;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SingleFileCodeResult {
    @JsonProperty("htmlCode")
    private String htmlCode;
    @JsonProperty("description")
    private String description;
}
