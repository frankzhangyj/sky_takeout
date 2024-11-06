package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;

import java.util.List;

/**
 * @Description
 * @Author frank
 * @Date 2024/10/29
 */
public interface SetmealService {
    /**
     * 条件查询 根据分类id查询套餐
     * @param categoryId
     * @return
     */
    List<Setmeal> list(Long categoryId);

    /**
     * 根据套餐id查询菜品选项
     * @param id
     * @return
     */
    List<DishItemVO> getDishItemById(Long id);

    /**
     * 向套餐中报错菜品信息
     * @param setmealDTO
     */
    void saveWithDish(Setmeal setmeal, SetmealDTO setmealDTO);

    /**
     * 分页查询套餐
     * @param setmealPageQueryDTO
     * @return
     */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 批量删除套餐
     * @param ids
     */
    void removeSetmeals(List<Long> ids);

    /**
     * 根据菜品id
     * @param id
     * @return
     */
    SetmealVO getByIdWithDish(Long id);

    /**
     * 更新套餐
     * @param setmealDTO
     */
    void updateSetmeal(Setmeal setmeal, SetmealDTO setmealDTO);

    /**
     * 启售禁售套餐
     * @param status
     * @param id
     */
    void enableOrDisableSetmeal(Integer status, Long id);
}
