package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.vo.DishItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealMapper extends BaseMapper<Setmeal> {
    /**
     * 根据套餐id查询菜品
     * @param setmealId
     * @return
     */
    @Select("select sd.name, sd.copies, d.image, d.description " +
            "from setmeal_dish sd left join dish d on sd.dish_id = d.id " +
            "where sd.setmeal_id = #{setmealId}")
    List<DishItemVO> selectDishItemBySetmealId(Long setmealId);

    /**
     * 分页查询套餐
     * @param page
     * @param setmealPageQueryDTO
     */
    Page<Setmeal> selectSetmealPage(IPage<Setmeal> page, SetmealPageQueryDTO setmealPageQueryDTO);
}