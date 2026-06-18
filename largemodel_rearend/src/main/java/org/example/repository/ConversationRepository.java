/**
 * 模块：对话管理
 * 功能：对话会话数据访问层，JPA自动实现
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.repository;

import org.example.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Page<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    List<Conversation> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, Integer status);

    Optional<Conversation> findByIdAndUserId(Long id, Long userId);
}
