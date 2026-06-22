/**
 * 操作日志查询 API
 */
package org.example.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.dto.response.ApiResponse;
import org.example.dto.response.PageResponse;
import org.example.entity.OperationLog;
import org.example.mapper.OperationLogMapper;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class LogController {

    private final OperationLogMapper logMapper;

    @GetMapping("/logs")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword) {
        IPage<OperationLog> p = logMapper.search(Page.of(page, size), level, keyword);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (OperationLog l : p.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("timestamp", l.getCreatedAt() != null ? l.getCreatedAt().toString().substring(0, 19) : "");
            m.put("level", l.getModule() != null ? l.getModule().toUpperCase() : "INFO");
            m.put("module", l.getModule());
            m.put("message", (l.getOperatorName() != null ? l.getOperatorName() : "") + " " + l.getAction() + " " + (l.getTarget() != null ? l.getTarget() : "") + (l.getDetail() != null ? " | " + l.getDetail().substring(0, Math.min(100, l.getDetail().length())) : ""));
            m.put("user", l.getOperatorName());
            rows.add(m);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", rows);
        result.put("totalElements", p.getTotal());
        result.put("totalPages", p.getPages());
        result.put("currentPage", page);
        result.put("pageSize", size);
        return ApiResponse.success(result);
    }
}
