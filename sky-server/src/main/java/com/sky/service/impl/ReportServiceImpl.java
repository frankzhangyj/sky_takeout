package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
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
}
