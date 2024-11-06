package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.annotation.AutoFill;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description
 * @Author frank
 * @Date 2024/10/29
 */
@Service
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    @Override
    public List<Setmeal> list(Long categoryId) {
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Setmeal::getCategoryId, categoryId)
                .eq(Setmeal::getStatus, StatusConstant.ENABLE);

        List<Setmeal> setmeals = setmealMapper.selectList(queryWrapper);

        return setmeals;
    }

    @Override
    public List<DishItemVO> getDishItemById(Long id) {
        List<DishItemVO> dishItemVOS = setmealMapper.selectDishItemBySetmealId(id);
        return dishItemVOS;
    }

    @Override
    @Transactional
    @AutoFill(OperationType.INSERT)
    public void saveWithDish(Setmeal setmeal, SetmealDTO setmealDTO) {
        setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        // 插入套餐表
        setmealMapper.insert(setmeal);

        Long id = setmeal.getId();

        // 修改套餐菜品关系表
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(id);
            setmealDishMapper.insert(setmealDish);
        });
    }

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        IPage<Setmeal> page = new Page<>(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());

        setmealMapper.selectSetmealPage(page, setmealPageQueryDTO);

        return new PageResult(page.getTotal(), page.getRecords());
    }

    @Override
    public void removeSetmeals(List<Long> ids) {
        // 启售中的套餐不能删除
        ids.forEach(id -> {
            Setmeal setmeal = setmealMapper.selectById(id);
            if(StatusConstant.ENABLE == setmeal.getStatus()){
                //起售中的套餐不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });

        // 删除套餐表和套餐菜品关系表中的数据
        setmealMapper.deleteBatchIds(ids);
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SetmealDish::getSetmealId, ids);
        setmealDishMapper.delete(queryWrapper);
    }

    @Override
    public SetmealVO getByIdWithDish(Long id) {
        // 根据套餐id查询套餐和套餐包含的菜品
        Setmeal setmeal = setmealMapper.selectById(id);
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, id);
        List<SetmealDish> setmealDishes = setmealDishMapper.selectList(queryWrapper);

        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);

        return setmealVO;
    }

    @Override
    @Transactional
    @AutoFill(OperationType.UPDATE)
    public void updateSetmeal(Setmeal setmeal, SetmealDTO setmealDTO) {
        BeanUtils.copyProperties(setmealDTO, setmeal);
        // 更新套餐表
        setmealMapper.updateById(setmeal);

        // 删除原来的套餐菜品关系表
        Long setmealId = setmealDTO.getId();

        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmealId);
        setmealDishMapper.delete(queryWrapper);

        // 重新设置套餐菜品关系
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
            setmealDishMapper.insert(setmealDish);
        });
    }

    @Override
    public void enableOrDisableSetmeal(Integer status, Long id) {
        // 当套餐启售时包含的菜品不能有禁售
        if (status == StatusConstant.ENABLE) {
            // 查询套餐包含的菜品
            LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SetmealDish::getSetmealId, id);
            List<SetmealDish> setmealDishes = setmealDishMapper.selectList(queryWrapper);

            List<Long> dishIds = new ArrayList<>();
            setmealDishes.forEach(setmealDish -> {
                dishIds.add(setmealDish.getDishId());
            });
            // 查询是否存在菜品禁售
            LambdaQueryWrapper<Dish> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.eq(Dish::getStatus, StatusConstant.DISABLE)
                    .in(Dish::getId, dishIds);
            List<Dish> dishes = dishMapper.selectList(queryWrapper1);

            if (dishes != null && dishes.size() > 0) {
                throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
            }
        }

        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.updateById(setmeal);
    }
}
