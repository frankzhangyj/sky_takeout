package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

/**
 * @Description
 * @Author frank
 * @Date 2024/10/30
 */
public interface ShoppingCartService {

    /**
     * 添加菜品或者套餐到购物车
     * @param shoppingCartDTO
     */
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    /**
     * 查询购物车表中所有数据
     * @return
     */
    List<ShoppingCart> getCarts();

    /**
     * 清空购物车中的信息
     */
    void removeShoppingCart();
}
