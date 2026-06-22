package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.entity.OperationLog;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    @Select({"<script>",
            "SELECT * FROM operation_logs WHERE 1=1",
            "<if test='level != null and level != \"\"'> AND module = #{level}</if>",
            "<if test='keyword != null and keyword != \"\"'> AND (detail LIKE CONCAT('%',#{keyword},'%') OR operator_name LIKE CONCAT('%',#{keyword},'%'))</if>",
            "ORDER BY created_at DESC",
            "</script>"})
    IPage<OperationLog> search(Page<OperationLog> page, @Param("level") String level, @Param("keyword") String keyword);
}
