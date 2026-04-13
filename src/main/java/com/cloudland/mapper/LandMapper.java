package com.cloudland.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudland.pojo.Land;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudland.pojo.vo.LandVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author longlive
 * @since 2023-09-02
 */
public interface LandMapper extends BaseMapper<Land> {

    List<LandVO> selectAll();
    LandVO selectById(Integer id);

    Page<LandVO> selectByCondition(@Param("page")Page<LandVO> page, @Param("land")Land land);
}
