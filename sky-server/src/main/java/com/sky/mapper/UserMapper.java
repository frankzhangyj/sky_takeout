package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Description
 * @Author frank
 * @Date 2024/10/29
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
