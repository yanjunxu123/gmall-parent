/**
 * @author yjx
 * @date 2021年 12月18日 13:50:11
 */
package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;
import feign.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import java.util.Map;

@Controller
public class ItemController {
    @Resource
    private ItemFeignClient itemFeignClient;

    @RequestMapping("{skuId}.html")
    public String getItem(@PathVariable Long skuId, Model model){
        Result<Map> result = itemFeignClient.getItem(skuId);
        model.addAllAttributes(result.getData());
        return "item/item";
    }
}

