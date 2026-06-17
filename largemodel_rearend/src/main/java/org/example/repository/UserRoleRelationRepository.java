/**
 * 模块：用户体系
 * 功能：用户角色关联数据访问层，JPA自动实现，提供按用户ID查询/删除关联
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.repository;

import org.example.entity.UserRoleRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRelationRepository extends JpaRepository<UserRoleRelation, Long> {

    List<UserRoleRelation> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
