/**
 * @author yjx
 * @date 2021年 12月14日 16:40:32
 */
package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.PushbackInputStream;
import java.util.List;

@RestController
@RequestMapping("admin/product/baseCategoryTrademark")
public class BaseCategoryTrademarkController {
    @Autowired
    private BaseCategoryTrademarkService baseCategoryTrademarkService;

    /**
     * 保存分类与品牌的关系
     * @param categoryTrademarkVo
     * @return
     */
    @PostMapping("save")
    public Result saveBaseCategoryTrademark(@RequestBody CategoryTrademarkVo categoryTrademarkVo){
        baseCategoryTrademarkService.saveBaseCategoryTrademark(categoryTrademarkVo);
        return Result.ok();
    }

    /**
     * 根据三级分类id和品牌id删除 分类与品牌的关系
     * @param category3Id
     * @param trademarkId
     * @return
     */
    @DeleteMapping("remove/{category3Id}/{trademarkId}")
    public Result deleteBaseCategoryTrademark(@PathVariable Long category3Id,
                                              @PathVariable Long trademarkId){
        baseCategoryTrademarkService.deleteBaseCategoryTrademark(category3Id, trademarkId);
        return Result.ok();
    }

    /**
     * 根据三级分类id获取所有的品牌列表
     * @param category3Id
     * @return
     */
    @GetMapping("/findTrademarkList/{category3Id}")
    public Result findTrademarkList(@PathVariable Long category3Id){
        List<BaseTrademark> list = baseCategoryTrademarkService.findTrademarkList(category3Id);
        return Result.ok(list);
    }

    /**
     * 根据三级分类id获取可选的品牌列表
      * @param category3Id
     * @return
     */
    @GetMapping("/findCurrentTrademarkList/{category3Id}")
    public Result findCurrentTrademarkList(@PathVariable Long category3Id){
        List<BaseTrademark> list = baseCategoryTrademarkService.findCurrentTrademarkList(category3Id);
        return Result.ok(list);
    }
}

