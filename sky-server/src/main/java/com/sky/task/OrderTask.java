package com.sky.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.constant.MessageConstant;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自定义定时任务，实现订单状态定时处理
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理支付超时订单 每分钟
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder() {
        log.info("处理支付超时订单：{}", LocalDateTime.now());

        LocalDateTime localDateTime = LocalDateTime.now().plusMinutes(-15);
        // 查找未支付超过15分钟订单
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getPayStatus, Orders.PENDING_PAYMENT)
                .lt(Orders::getOrderTime, localDateTime);
        List<Orders> orders = orderMapper.selectList(queryWrapper);

        if (orders != null && orders.size() > 0) {
            orders.forEach(order -> {
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason(MessageConstant.PAYMENT_TIMEOUT);
                order.setCancelTime(LocalDateTime.now());
                orderMapper.updateById(order);
            });
        }
    }

    /**
     * 处理“派送中”状态的订单 每天一点检查
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder() {
        log.info("处理派送中订单：{}", LocalDateTime.now());
        // 查找配送时间超过1小时的订单
        LocalDateTime localDateTime = LocalDateTime.now().plusMinutes(-60);

        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS)
                .lt(Orders::getEstimatedDeliveryTime, localDateTime);
        List<Orders> orders = orderMapper.selectList(queryWrapper);

        if (orders != null && orders.size() > 0) {
            orders.forEach(order -> {
                order.setStatus(Orders.COMPLETED);
                orderMapper.updateById(order);
            });
        }
    }

}