/**
 * @author yjx
 * @date 2021年 12月29日 18:13:01
 */
package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CartController {
    @Autowired
    private ProductFeignClient productFeignClient;

    /**
     * 添加购物车
     * @param skuId
     * @param skuNum
     * @param model
     * @return
     */
    @RequestMapping("addCart.html")
    public String addCart(Long skuId,Integer skuNum, Model model) {
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        model.addAttribute("skuInfo",skuInfo);
        model.addAttribute("skuNum",skuNum);
        return "cart/addCart";
    }

    @GetMapping("cart.html")
    public String cartList(){
        return "cart/index";
    }
}

