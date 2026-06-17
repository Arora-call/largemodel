/**
 * 模块：对话管理
 * 功能：消息数据访问层，JPA自动实现
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.repository;

import org.example.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /** 游标分页：查询指定游标之前最近20条消息 */
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId AND m.id < :cursor ORDER BY m.id DESC")
    List<Message> findRecentBeforeCursor(@Param("conversationId") Long conversationId,
                                          @Param("cursor") Long cursor,
                                          Pageable pageable);
}
