package com.sky.service;

import com.sky.dto.ShoppingCartDTO;

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
}
