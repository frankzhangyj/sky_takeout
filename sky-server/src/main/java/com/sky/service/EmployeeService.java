package com.sky.service;

import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.dto.PasswordEditDTO;
import com.sky.entity.Employee;
import com.sky.result.PageResult;
import com.sky.result.Result;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 保存员工信息
     * @param employeeDTO
     */
    void saveEmployee(Employee employee, EmployeeDTO employeeDTO);

    /**
     * 分页查询员工
     * @param employeePageQueryDTO
     * @return
     */
    PageResult getPage(EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * 启用禁用员工
     * @param status
     * @param id
     */
    void enableOrDisable(Employee employee, Integer status, Long id);

    /**
     * 查询员工信息
     * @param id
     * @return
     */
    Employee getById(Long id);

    /**
     * 更新员工信息
     * @param employeeDTO
     */
    void updateEmployeeDetail(Employee employee, EmployeeDTO employeeDTO);

    /**
     * 修改用户密码
     * @param employee
     * @param passwordEditDTO
     * @return
     */
    Result updateEmployeePWD(Employee employee, PasswordEditDTO passwordEditDTO);
}
