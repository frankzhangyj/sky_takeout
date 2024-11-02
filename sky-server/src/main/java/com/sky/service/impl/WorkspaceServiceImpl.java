package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.entity.Orders;
import com.sky.entity.Setmeal;
import com.sky.entity.User;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;

/**
 * @Description
 * @Author frank
 * @Date 2024/11/2
 */
@Service
public class WorkspaceServiceImpl implements WorkspaceService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
        // 订单数
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.gt(Orders::getOrderTime, begin)
                .lt(Orders::getOrderTime, end);

        Integer totalOrderCount = orderMapper.selectCount(queryWrapper).intValue();

        // 营业额
        Map map = new HashMap();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", Orders.COMPLETED);

        Double turnover = orderMapper.sumByMap(map);
        turnover = turnover == null ? 0.0 : turnover;

        // 有效订单数
        queryWrapper.eq(Orders::getStatus, Orders.COMPLETED);
        Integer validOrderCount = orderMapper.selectCount(queryWrapper).intValue();

        Double unitPrice = 0.0;

        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0 && validOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
            unitPrice = turnover / validOrderCount;
        }

        // 新增用户
        LambdaQueryWrapper<User> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.gt(begin != null, User::getCreateTime, begin)
                .lt(end != null, User::getCreateTime, end);

        Integer newUsers = userMapper.selectCount(queryWrapper1).intValue();

        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }

    @Override
    public OrderOverViewVO getOrderOverView() {
        LocalDateTime begin = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime end = LocalDateTime.now();
        // 待接单
        Integer waitingOrders = getOrderCount(begin, end, Orders.TO_BE_CONFIRMED);
        // 待派送
        Integer deliveredOrders = getOrderCount(begin, end, Orders.CONFIRMED);
        // 已完成
        Integer completedOrders = getOrderCount(begin, end, Orders.COMPLETED);
        // 已取消
        Integer cancelledOrders = getOrderCount(begin, end, Orders.CANCELLED);
        // 总共
        Integer totalOrders = getOrderCount(begin, end, null);

        return OrderOverViewVO.builder()
                .waitingOrders(waitingOrders)
                .deliveredOrders(deliveredOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(totalOrders)
                .build();
    }

    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status) {
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.gt(begin != null, Orders::getOrderTime, begin)
                .lt(end != null, Orders::getOrderTime, end)
                .eq(status != null, Orders::getStatus, status);

        Integer orderCount = orderMapper.selectCount(queryWrapper).intValue();

        return orderCount;
    }

    @Override
    public DishOverViewVO getDishOverView() {
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Dish::getStatus, StatusConstant.ENABLE);
        Integer enabled = dishMapper.selectCount(queryWrapper).intValue();

        LambdaQueryWrapper<Dish> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(Dish::getStatus, StatusConstant.DISABLE);
        Integer disabled = dishMapper.selectCount(queryWrapper1).intValue();

        return DishOverViewVO.builder()
                .sold(enabled)
                .discontinued(disabled)
                .build();
    }

    @Override
    public SetmealOverViewVO getSetmealOverView() {
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Setmeal::getStatus, StatusConstant.ENABLE);
        Integer enabled = setmealMapper.selectCount(queryWrapper).intValue();

        LambdaQueryWrapper<Setmeal> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(Setmeal::getStatus, StatusConstant.DISABLE);
        Integer disabled = setmealMapper.selectCount(queryWrapper1).intValue();

        return SetmealOverViewVO.builder()
                .sold(enabled)
                .discontinued(disabled)
                .build();
    }
}
