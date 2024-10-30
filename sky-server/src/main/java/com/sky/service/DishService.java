package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {

    /**
     * 新增菜品和对应的口味
     *
     * @param dishDTO
     */
    void saveWithFlavor(Dish dish, DishDTO dishDTO);

    /**
     * 分页查询
     * @param dishPageQueryDTO
     * @return
     */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 批量删除
     * @param ids
     */
    void removeDishes(List<Long> ids);

    /**
     * 启售禁售
     * @param status
     * @param id
     */
    void enableOrDishableDish(Integer status, Long id);

    /**
     * 得到DsihVO用于回显
     * @param id
     * @return
     */
    DishVO getByIdWithFlavor(Long id);

    /**
     * 修改菜品
     * @param dish
     * @param dishDTO
     */
    void updateWithFlavor(Dish dish, DishDTO dishDTO);

    /**
     * 根据分类id查询所有菜品带所有菜品所有口味
     * 通过redis缓存减少数据库查询
     * @param categoryId
     * @return
     */
    List<DishVO> listWithFlavor(Long categoryId);

    /**
     * 根据分类id查询菜品的所有信息
     * @param categoryId
     * @return
     */
    List<Dish> listDishes(Long categoryId);
}