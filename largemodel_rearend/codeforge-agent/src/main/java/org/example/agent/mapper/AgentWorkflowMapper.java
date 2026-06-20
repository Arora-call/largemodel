/**
 * Agent 工作流 Mapper
 */
package org.example.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.entity.AgentWorkflow;

import java.util.List;

@Mapper
public interface AgentWorkflowMapper extends BaseMapper<AgentWorkflow> {

    @Select("SELECT * FROM agent_workflows WHERE user_id = #{userId} ORDER BY updated_at DESC")
    IPage<AgentWorkflow> findByUserId(Page<AgentWorkflow> page, @Param("userId") Long userId);

    @Select("SELECT * FROM agent_workflows WHERE user_id = #{userId} AND status = #{status} ORDER BY updated_at DESC")
    List<AgentWorkflow> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM agent_workflows WHERE user_id = #{userId}")
    long countByUserId(@Param("userId") Long userId);
}
