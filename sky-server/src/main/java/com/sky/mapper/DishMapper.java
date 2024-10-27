package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {
    IPage<DishVO> selectDishPage(IPage<DishVO> page, @Param("dishPageQueryDTO") DishPageQueryDTO dishPageQueryDTO);

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
//    @Select("select count(id) from dish where category_id = #{categoryId}")
//    Integer countByCategoryId(Long categoryId);

}
