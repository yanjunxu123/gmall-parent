/**
 * @author yjx
 * @date 2021年 12月14日 13:19:03
 */
package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/product/baseTrademark")
public class BaseTrademarkController {
    @Autowired
    private BaseTrademarkService baseTrademarkService;

    /**
     * 保存（新增）品牌
     * @param baseTrademark
     * @return
     */
    @PostMapping("save")
    public Result save(@RequestBody BaseTrademark baseTrademark){
        baseTrademarkService.save(baseTrademark);
       return Result.ok();
    }

    /**
     * 品牌的删除
     * @param id
     * @return
     */
    @DeleteMapping("/remove/{id}")
    public Result delete(@PathVariable() Long id){
        baseTrademarkService.removeById(id);
        return Result.ok();
    }

    /**
     * 品牌分页
     * @param page
     * @param limit
     * @return
     */
    @GetMapping("/{page}/{limit}")
    public Result getBaseTrademarkPage(@PathVariable Long page,
                                       @PathVariable Long limit){
        Page<BaseTrademark> baseTrademarkPage = new Page<>(page, limit);
        IPage iPage = baseTrademarkService.getBaseTrademarkPage(baseTrademarkPage);
        return Result.ok(iPage);
    }

    /**
     * 品牌更新
     * @param baseTrademark
     * @return
     */
    @PostMapping("update")
    public Result updateTrademarkPage(@RequestBody BaseTrademark baseTrademark){
        baseTrademarkService.updateById(baseTrademark);
        return Result.ok();
    }

    /**
     * 根据品牌id获取品牌
     * @param id
     * @return
     */
    @GetMapping("get/{id}")
    public Result getTrademarkById(@PathVariable() String id){
        BaseTrademark baseTrademark = baseTrademarkService.getById(id);
        return Result.ok(baseTrademark);
    }
}

