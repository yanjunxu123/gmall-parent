/**
 * @author yjx
 * @date 2021年 12月28日 18:22:36
 */
package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("api/cart")
public class CartApiController {
    @Autowired
    private CartService cartService;

    /**
     * 添加购物车
     * @param skuId
     * @param skuNum
     * @param request
     * @return
     */
    @RequestMapping("addToCart/{skuId}/{skuNum}")
    public Result addCart(@PathVariable Long skuId,
                          @PathVariable Integer skuNum,
                          HttpServletRequest request) {
        //获取userId
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
          userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.addCart(skuId,userId, skuNum);
        return Result.ok();
    }

    /**
     * 查询购物车列表
     * @param request
     * @return
     */
    @GetMapping("cartList")
    public Result cartList(HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        String userTempId = AuthContextHolder.getUserTempId(request);
        List<CartInfo> cartInfoList = cartService.getCartList(userId,userTempId);
        return Result.ok(cartInfoList);
    }

    /**
     * 更新购物车的选中状态
     * @param skuId
     * @param isChecked
     * @return
     */
    @GetMapping("checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable Long skuId,
                            @PathVariable Integer isChecked,
                            HttpServletRequest request){
        //获取userId
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.checkCart(skuId,userId, isChecked);
        return Result.ok();
    }


    /**
     * 删除购物车
     * @param skuId
     * @param request
     * @return
     */
    @DeleteMapping("deleteCart/{skuId}")
    public Result deleteCart(@PathVariable Long skuId,
                             HttpServletRequest request){
        //获取userId
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.deleteCart(userId,skuId);
        return Result.ok();
    }

    /**
     * 查询选中状态的购物车列表
     * @param userId
     * @return
     */
    @GetMapping("getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable String userId){
        return cartService.getCartCheckedList(userId);
    }

}

