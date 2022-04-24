/**
 * @author yjx
 * @date 2021年 12月14日 08:55:36
 */
package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseSaleAttr;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/product")
public class SpuManageController {

    @Autowired
    private ManageService manageService;

    /**
     * SpuInfo分页列表
     * 从url中获取参数3种方式：
     * ①对象传值：url中参数的名称与对象中属性名称一致才行。
     * ②@RequestParam
     * ③HttpServletRequest request接收参数 request.getParam
     * @param page
     * @param limit
     * @param spuInfo
     * @return
     */
    @GetMapping("{page}/{limit}")
    public Result getSpuInfoPage(@PathVariable Long page,
                                 @PathVariable Long limit,
                                 SpuInfo spuInfo){
        Page<SpuInfo> spuInfoPage = new Page<>(page,limit);

        IPage<SpuInfo> spuInfoPageList = manageService.getSpuInfoPage(spuInfoPage, spuInfo);
        return Result.ok(spuInfoPageList);
    }

    /**
     * 查询所有销售属性
     * @return
     */
    @GetMapping("/baseSaleAttrList")
    public Result baseSaleAttrList(){
      List<BaseSaleAttr> baseSaleAttrList = manageService.getBaseSaleAttrList();
      return Result.ok(baseSaleAttrList);
    }

    /**
     * 保存spu
     * @param spuInfo
     * @return
     */
    @PostMapping("/saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo) {
                manageService.saveSpuInfo(spuInfo);
                return Result.ok();
    }

}

