/**
 * AI 模型配置 Mapper
 */
package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.entity.ModelConfig;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ModelConfigMapper extends BaseMapper<ModelConfig> {

    @Select("SELECT * FROM model_configs WHERE is_enabled = 1 ORDER BY sort_order ASC")
    List<ModelConfig> findAllEnabled();

    @Select("SELECT * FROM model_configs WHERE is_enabled = 1 AND is_default = 1 LIMIT 1")
    Optional<ModelConfig> findDefault();

    @Select("SELECT * FROM model_configs WHERE is_enabled = 1 AND id = #{id}")
    Optional<ModelConfig> findEnabledById(Long id);
}
