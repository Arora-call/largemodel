/**
 * 模块：对话管理
 * 功能：消息表 MyBatis-Plus Mapper（与 JPA MessageRepository 并行，逐步迁移）
 * 作者：yx
 * 创建时间：2026-06-20
 */
package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.entity.Message;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Select("SELECT * FROM messages WHERE conversation_id = #{conversationId} ORDER BY created_at ASC")
    List<Message> findByConversationIdOrderByCreatedAtAsc(@Param("conversationId") Long conversationId);

    @Select("SELECT * FROM messages WHERE conversation_id = #{conversationId} AND id < #{cursor} ORDER BY id DESC LIMIT 20")
    List<Message> findRecentBeforeCursor(@Param("conversationId") Long conversationId,
                                          @Param("cursor") Long cursor);

    @Delete("DELETE FROM messages WHERE conversation_id = #{conversationId}")
    int deleteByConversationId(@Param("conversationId") Long conversationId);
}
