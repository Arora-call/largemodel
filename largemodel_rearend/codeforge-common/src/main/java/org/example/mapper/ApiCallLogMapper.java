package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.entity.ApiCallLog;
import java.util.List;
import java.util.Map;

@Mapper
public interface ApiCallLogMapper extends BaseMapper<ApiCallLog> {

    @Select("SELECT COUNT(*) FROM api_call_logs WHERE created_at >= DATE(NOW())")
    long countToday();

    @Select("SELECT COALESCE(AVG(CASE WHEN success=1 THEN 1 ELSE 0 END)*100,0) FROM api_call_logs WHERE created_at >= DATE(NOW())")
    double successRate();

    @Select("SELECT COALESCE(SUM(token_used),0) FROM api_call_logs WHERE created_at >= DATE(NOW())")
    long todayTokens();

    @Select("SELECT COALESCE(AVG(latency_ms),0) FROM api_call_logs WHERE created_at >= DATE(NOW()) AND success=1")
    long avgLatency();

    @Select("SELECT DATE(created_at) as dt, COUNT(*) as cnt FROM api_call_logs " +
            "WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "GROUP BY dt ORDER BY dt")
    List<Map<String, Object>> dailyCalls(@Param("days") int days);

    @Select("SELECT DATE(created_at) as dt, COALESCE(SUM(token_used),0) as tokens FROM api_call_logs " +
            "WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "GROUP BY dt ORDER BY dt")
    List<Map<String, Object>> dailyTokens(@Param("days") int days);

    @Select("SELECT model_name as name, COUNT(*) as value FROM api_call_logs " +
            "WHERE created_at >= DATE(NOW()) GROUP BY model_name")
    List<Map<String, Object>> modelDistribution();
}
