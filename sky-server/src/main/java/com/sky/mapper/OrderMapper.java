package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Description
 * @Author frank
 * @Date 2024/10/30
 */
@Mapper
public interface OrderMapper extends BaseMapper<Orders> {
}
