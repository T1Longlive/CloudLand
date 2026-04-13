package com.cloudland.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudland.pojo.Msg;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author longlive
 * @since 2024-02-23
 */
public interface MsgMapper extends BaseMapper<Msg> {
    Page<Msg> selectByCondition(@Param("page")Page<Msg> page, @Param("msg") Msg msg);
}
