package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * @Description
 * @Author frank
 * @Date 2024/10/30
 */
@Mapper
public interface OrderMapper extends BaseMapper<Orders> {
    /**
     * 根据状态统计订单数量
     * @param status
     */
    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer status);

    /**
     * 计算每一天营业额
     * @param map
     * @return
     */
    Double sumByMap(Map map);
}
