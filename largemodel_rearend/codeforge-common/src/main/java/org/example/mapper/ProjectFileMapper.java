/**
 * 模块：AI代码生成
 * 功能：项目文件 Mapper，管理 @Tool 工具创建的项目文件
 * 作者：yx
 * 创建时间：2026-06-24
 */
package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.entity.ProjectFile;

import java.util.List;

@Mapper
public interface ProjectFileMapper extends BaseMapper<ProjectFile> {

    @Select("SELECT * FROM project_files WHERE conversation_id = #{conversationId} ORDER BY file_path")
    List<ProjectFile> findByConversationId(@Param("conversationId") Long conversationId);

    @Select("SELECT * FROM project_files WHERE conversation_id = #{conversationId} AND file_path = #{filePath} LIMIT 1")
    ProjectFile findByConversationAndPath(@Param("conversationId") Long conversationId,
                                          @Param("filePath") String filePath);

    @Delete("DELETE FROM project_files WHERE conversation_id = #{conversationId}")
    int deleteByConversationId(@Param("conversationId") Long conversationId);
}
