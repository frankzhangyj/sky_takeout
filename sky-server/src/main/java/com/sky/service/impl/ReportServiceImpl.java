package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author frank
 * @Date 2024/11/1
 */
@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public TurnoverReportVO getTurnover(LocalDate beginTime, LocalDate endTime) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(beginTime);

        if (beginTime.isAfter(endTime)) {
            throw new RuntimeException("时间错误");
        }

        while (!beginTime.equals(endTime)) {
            beginTime = beginTime.plusDays(1);
            dateList.add(beginTime);
        }

        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate localDate : dateList) {
            LocalDateTime begin = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime end = LocalDateTime.of(localDate, LocalTime.MAX);
            // 获取当天的营业额
            Map map = new HashMap();
            map.put("begin", begin);
            map.put("status", Orders.COMPLETED);
            map.put("end", end);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> newUserList = new ArrayList<>(); //新增用户数
        List<Integer> totalUserList = new ArrayList<>(); //总用户数

        for (LocalDate localDate : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);

            Integer newUser = getUserCount(beginTime, endTime);
            Integer totalUser = getUserCount(null, endTime);

            newUserList.add(newUser);
            totalUserList.add(totalUser);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }

    private Integer getUserCount(LocalDateTime begin, LocalDateTime end) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.gt(begin != null, User::getCreateTime, begin)
                .lt(end != null, User::getCreateTime, end);

        Integer users = userMapper.selectCount(queryWrapper).intValue();

        return users;
    }
}
