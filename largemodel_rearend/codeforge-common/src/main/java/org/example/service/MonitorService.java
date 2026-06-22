/**
 * 监控统计服务
 */
package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.ApiCallLog;
import org.example.mapper.ApiCallLogMapper;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MonitorService {

    private final ApiCallLogMapper logMapper;

    /** 记录一次 API 调用 */
    public void record(String endpoint, Long userId, String model, int tokens, long latencyMs, boolean success, String error) {
        ApiCallLog log = ApiCallLog.builder()
                .endpoint(endpoint).userId(userId).modelName(model)
                .tokenUsed(tokens).latencyMs(latencyMs).success(success)
                .errorMsg(error).build();
        logMapper.insert(log);
    }

    /** 概览统计 */
    public Map<String, Object> overview() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("todayCalls", logMapper.countToday());
        m.put("successRate", Math.round(logMapper.successRate() * 10) / 10.0);
        m.put("todayTokens", logMapper.todayTokens());
        m.put("avgLatency", logMapper.avgLatency());
        return m;
    }

    /** 每日调用量 */
    public List<Map<String, Object>> callTrend(int days) {
        return logMapper.dailyCalls(days);
    }

    /** 每日 Token */
    public List<Map<String, Object>> tokenTrend(int days) {
        return logMapper.dailyTokens(days);
    }

    /** 模型分布 */
    public List<Map<String, Object>> modelDist() {
        return logMapper.modelDistribution();
    }
}
