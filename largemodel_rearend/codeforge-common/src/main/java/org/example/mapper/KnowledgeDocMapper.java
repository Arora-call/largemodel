/**
 * 模块：知识库（通用）
 * 功能：知识库文档精简 Mapper，供 AI 生成检索知识库内容
 * 作者：yx
 * 创建时间：2026-06-25
 */
package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.entity.KnowledgeDocument;

import java.util.List;

@Mapper
public interface KnowledgeDocMapper extends BaseMapper<KnowledgeDocument> {

    /** 按 ID 列表查询有效文档 */
    @Select("<script>" +
            "SELECT * FROM knowledge_documents WHERE status = 1 AND id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<KnowledgeDocument> findByIds(@Param("ids") List<Long> ids);

    /** 关键词检索（用于 AI 自动匹配） */
    @Select("SELECT * FROM knowledge_documents WHERE user_id = #{userId} AND status = 1 " +
            "AND (title LIKE CONCAT('%',#{keyword},'%') OR content LIKE CONCAT('%',#{keyword},'%')) " +
            "ORDER BY updated_at DESC LIMIT #{limit}")
    List<KnowledgeDocument> searchForAi(@Param("userId") Long userId,
                                        @Param("keyword") String keyword,
                                        @Param("limit") int limit);
}
