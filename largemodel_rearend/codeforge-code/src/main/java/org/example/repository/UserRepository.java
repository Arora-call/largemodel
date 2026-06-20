/**
 * 模块：用户体系
 * 功能：用户数据访问层，JPA自动实现，提供用户名/ID查询、存在性检查、统计等方法
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.repository;

import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsernameAndDeletedFalse(String username);

    Optional<User> findByIdAndDeletedFalse(Long id);

    boolean existsByUsernameAndDeletedFalse(String username);

    boolean existsByUsernameAndIdNotAndDeletedFalse(String username, Long id);

    long countByDeletedFalse();
}
