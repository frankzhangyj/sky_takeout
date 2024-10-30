package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.annotation.AutoFill;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description
 * @Author frank
 * @Date 2024/10/27
 */
@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    @AutoFill(OperationType.INSERT)
    @Transactional
    @Override
    public void saveWithFlavor(Dish dish, DishDTO dishDTO) {
        BeanUtils.copyProperties(dishDTO, dish);

        dishMapper.insert(dish);

        Long id = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        // 批量插入口味 不能使用in
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(id);
                dishFlavorMapper.insert(dishFlavor);
            });
        }
    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        IPage<DishVO> page = new Page<>(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        dishMapper.selectDishPage(page, dishPageQueryDTO);

        return new PageResult(page.getTotal(), page.getRecords());
    }

    @Override
    @Transactional
    public void removeDishes(List<Long> ids) {
        for (Long id : ids) {
            Dish dish = dishMapper.selectById(id);
            if (dish.getStatus() == StatusConstant.ENABLE) {
                //当前菜品处于起售中，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }

            LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SetmealDish::getDishId, id);
            List<SetmealDish> setmealDishes = setmealDishMapper.selectList(queryWrapper);

            if (setmealDishes != null && setmealDishes.size() > 0) {
                //当前菜品被套餐关联了，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
            }
        }

        // 使用in优化批量删除
        dishMapper.deleteBatchIds(ids);
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(DishFlavor::getDishId, ids);
        dishFlavorMapper.delete(queryWrapper);
    }

    @Override
    @AutoFill(OperationType.UPDATE)
    public void enableOrDishableDish(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();

        dishMapper.updateById(dish);
    }

    @Override
    public DishVO getByIdWithFlavor(Long id) {
        Dish dish = dishMapper.selectById(id);

        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, id);
        List<DishFlavor> dishFlavors = dishFlavorMapper.selectList(queryWrapper);

        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);

        return dishVO;
    }

    @Override
    @AutoFill(OperationType.UPDATE)
    public void updateWithFlavor(Dish dish, DishDTO dishDTO) {
        BeanUtils.copyProperties(dishDTO, dish);

        dishMapper.updateById(dish);
    }

    @Override
    public List<DishVO> listWithFlavor(Long categoryId) {
        // 查找缓存中是否存在这个类型下的菜品集合
        String key = "dish_" + categoryId;
        List<DishVO> dishVOS = (List<DishVO>) redisTemplate.opsForValue().get(key);

        if (dishVOS != null) {
            return dishVOS;
        }

        // 根据分类id得到启售中的菜品
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Dish::getCategoryId, categoryId)
                .eq(Dish::getStatus, StatusConstant.ENABLE);

        List<Dish> dishes = dishMapper.selectList(queryWrapper);

        dishVOS = new ArrayList<>();

        // 得到每个菜品的口味
        for (Dish dish : dishes) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dish, dishVO);

            LambdaQueryWrapper<DishFlavor> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.eq(DishFlavor::getDishId, dish.getId());
            List<DishFlavor> dishFlavors = dishFlavorMapper.selectList(queryWrapper1);

            dishVO.setFlavors(dishFlavors);
            dishVOS.add(dishVO);
        }

        // 将查询的信息添加到缓存中
        redisTemplate.opsForValue().set(key, dishVOS);

        return dishVOS;
    }
}
