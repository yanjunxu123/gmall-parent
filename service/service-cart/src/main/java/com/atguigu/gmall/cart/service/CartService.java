package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

public interface CartService {
    /**
     * 添加购物车
     * @param skuId
     * @param userId
     * @param skuNum
     */
    void addCart(Long skuId, String userId, Integer skuNum);

    /**
     * 查询购物车列表
     * @param userId
     * @param userTempId
     * @return
     */
    List<CartInfo> getCartList(String userId, String userTempId);

    /**
     * 更新购物车选择状态
     * @param skuId
     * @param isChecked
     */
    void checkCart(Long skuId, String userId,Integer isChecked);

    /**
     * 删除购物车
     * @param userId
     * @param skuId
     */
    void deleteCart(String userId,Long skuId);

    /**
     * 获取选中状态的购物车列表
     * @param userId
     */
    List<CartInfo> getCartCheckedList(String userId);
}
