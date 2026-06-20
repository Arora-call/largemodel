/**
 * 模块：应用管理
 * 功能：应用表 MyBatis-Plus Mapper（与 JPA ApplicationRepository 并行，逐步迁移）
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
import org.example.entity.Application;

import java.util.List;

@Mapper
public interface ApplicationMapper extends BaseMapper<Application> {

    @Select("SELECT * FROM applications WHERE user_id = #{userId} AND status != 0 ORDER BY updated_at DESC")
    IPage<Application> findByUserId(Page<Application> page, @Param("userId") Long userId);

    @Select("SELECT * FROM applications WHERE user_id = #{userId} AND name LIKE CONCAT('%',#{keyword},'%') AND status != 0 ORDER BY updated_at DESC")
    IPage<Application> findByUserIdAndKeyword(Page<Application> page,
                                               @Param("userId") Long userId,
                                               @Param("keyword") String keyword);

    @Select("SELECT * FROM applications WHERE user_id = #{userId} AND language = #{language} AND status != 0 ORDER BY updated_at DESC")
    IPage<Application> findByUserIdAndLanguage(Page<Application> page,
                                                @Param("userId") Long userId,
                                                @Param("language") String language);

    @Select("SELECT * FROM applications WHERE user_id = #{userId} AND name LIKE CONCAT('%',#{keyword},'%') AND language = #{language} AND status != 0 ORDER BY updated_at DESC")
    IPage<Application> findByUserIdAndKeywordAndLanguage(Page<Application> page,
                                                          @Param("userId") Long userId,
                                                          @Param("keyword") String keyword,
                                                          @Param("language") String language);

    @Select("SELECT * FROM applications WHERE user_id = #{userId} AND status != 0")
    List<Application> findByUserIdAndStatusNot(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM applications WHERE user_id = #{userId} AND status != 0")
    long countByUserIdAndStatusNot(@Param("userId") Long userId);
}
