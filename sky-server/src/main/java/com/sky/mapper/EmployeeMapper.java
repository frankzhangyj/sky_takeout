package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {
    /**
     * 分页查询员工信息 按照时间降序排列
     * @param page
     * @param employeePageQueryDTO
     */
    IPage<Employee> selectMyPage(IPage<Employee> page, @Param("employeePageQueryDTO") EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * 根据用户名查询员工
     * @param username
     * @return
     */
//    @Select("select * from employee where username = #{username}")
//    Employee getByUsername(String username);

}
