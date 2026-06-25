/**
 * Vue3 项目模式 AI 结构化输出
 */
package org.example.dto.codegen;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class VueProjectCodeResult {
    @JsonProperty("files")
    private List<FileEntry> files;
    @JsonProperty("description")
    private String description;
    @JsonProperty("projectName")
    private String projectName;

    @Data
    public static class FileEntry {
        @JsonProperty("path")
        private String path;
        @JsonProperty("content")
        private String content;
    }
}
