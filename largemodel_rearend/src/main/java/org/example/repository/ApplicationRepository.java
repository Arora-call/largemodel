/**
 * 模块：应用管理
 * 功能：应用数据访问层，JPA自动实现
 * 作者：yx
 * 创建时间：2026-06-17
 * 修改记录：
 *  2026-06-17 初始化代码
 */
package org.example.repository;

import org.example.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Page<Application> findByUserIdAndStatusNotOrderByUpdatedAtDesc(Long userId, Integer status, Pageable pageable);

    List<Application> findByUserIdAndStatusNot(Long userId, Integer status);

    long countByUserIdAndStatusNot(Long userId, Integer status);
}
