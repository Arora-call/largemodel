/**
 * 模块：用户体系
 * 功能：用户表 MyBatis-Plus Mapper（与 JPA UserRepository 并行，逐步迁移）
 * 作者：yx
 * 创建时间：2026-06-20
 */
package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.entity.User;

import java.util.Optional;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM users WHERE username = #{username} AND deleted = 0")
    Optional<User> findByUsernameAndDeletedFalse(String username);

    @Select("SELECT * FROM users WHERE id = #{id} AND deleted = 0")
    Optional<User> findByIdAndDeletedFalse(Long id);

    @Select("SELECT COUNT(*) > 0 FROM users WHERE username = #{username} AND deleted = 0")
    boolean existsByUsernameAndDeletedFalse(String username);

    @Select("SELECT COUNT(*) > 0 FROM users WHERE username = #{username} AND id != #{id} AND deleted = 0")
    boolean existsByUsernameAndIdNotAndDeletedFalse(String username, Long id);

    @Select("SELECT COUNT(*) FROM users WHERE deleted = 0")
    long countByDeletedFalse();
}
