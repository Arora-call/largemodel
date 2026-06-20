/**
 * 知识库文档 Mapper
 */
package org.example.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.entity.KnowledgeDocument;

import java.util.List;

@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocument> {

    @Select("SELECT * FROM knowledge_documents WHERE user_id = #{userId} AND status = 1 ORDER BY updated_at DESC")
    IPage<KnowledgeDocument> findByUserId(Page<KnowledgeDocument> page, @Param("userId") Long userId);

    @Select("SELECT * FROM knowledge_documents WHERE user_id = #{userId} AND status = 1 AND (title LIKE CONCAT('%',#{keyword},'%') OR content LIKE CONCAT('%',#{keyword},'%')) ORDER BY updated_at DESC")
    IPage<KnowledgeDocument> search(Page<KnowledgeDocument> page, @Param("userId") Long userId, @Param("keyword") String keyword);

    @Select("SELECT * FROM knowledge_documents WHERE status = 1 AND collection = #{collection} ORDER BY updated_at DESC")
    List<KnowledgeDocument> findByCollection(@Param("collection") String collection);

    @Select("SELECT COUNT(*) FROM knowledge_documents WHERE user_id = #{userId} AND status = 1")
    long countByUserId(@Param("userId") Long userId);
}
