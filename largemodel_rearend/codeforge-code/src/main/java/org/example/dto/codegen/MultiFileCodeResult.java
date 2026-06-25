/**
 * 多文件模式 AI 结构化输出
 */
package org.example.dto.codegen;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MultiFileCodeResult {
    @JsonProperty("htmlCode")
    private String htmlCode;
    @JsonProperty("cssCode")
    private String cssCode;
    @JsonProperty("jsCode")
    private String jsCode;
    @JsonProperty("description")
    private String description;
}
