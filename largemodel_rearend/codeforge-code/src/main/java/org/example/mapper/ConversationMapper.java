/**
 * 模块：对话管理
 * 功能：对话会话表 MyBatis-Plus Mapper（与 JPA ConversationRepository 并行，逐步迁移）
 * 作者：yx
 * 创建时间：2026-06-20
 */
package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.entity.Conversation;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    @Select("SELECT * FROM conversations WHERE user_id = #{userId} ORDER BY updated_at DESC")
    IPage<Conversation> findByUserIdOrderByUpdatedAtDesc(Page<Conversation> page, @Param("userId") Long userId);

    @Select("SELECT * FROM conversations WHERE user_id = #{userId} AND status = #{status} ORDER BY updated_at DESC")
    List<Conversation> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);

    @Select("SELECT * FROM conversations WHERE user_id = #{userId} AND status = #{status} AND type = #{type} ORDER BY updated_at DESC")
    List<Conversation> findByUserIdAndStatusAndType(@Param("userId") Long userId,
                                                      @Param("status") Integer status,
                                                      @Param("type") String type);

    @Select("SELECT * FROM conversations WHERE id = #{id} AND user_id = #{userId}")
    Optional<Conversation> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE conversations SET status = 0 WHERE user_id = #{userId} AND status = 1")
    void deactivateByUserId(@Param("userId") Long userId);
}
