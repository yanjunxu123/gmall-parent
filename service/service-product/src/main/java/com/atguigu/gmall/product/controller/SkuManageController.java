/**
 * @author yjx
 * @date 2021年 12月15日 16:59:45
 */
package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/product")
public class SkuManageController {
    @Autowired
    private ManageService manageService;

    /**
     * 根据supId获取spuImage集合（sku添加界面回显spu的图片列表）
     * @param spuId
     * @return
     */
    @GetMapping("/spuImageList/{spuId}")
    public Result<List<SpuImage>> getSpuImageList(@PathVariable Long spuId) {
       List<SpuImage> spuImageList = manageService.getSpuImageList(spuId);
       return Result.ok(spuImageList);
    }

    /**
     * 根据spuId获取销售属性（sku添加界面回显销售属性）
     * @param spuId
     * @return
     */
    @GetMapping("/spuSaleAttrList/{spuId}")
    public Result<List<SpuSaleAttr>> getSpuSaleAttrList(@PathVariable Long spuId) {
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrList(spuId);
        return Result.ok(spuSaleAttrList);
    }

    /**
     * 保存sku
     * @param skuInfo
     * @return
     */
    @PostMapping("/saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo){
        manageService.saveSkuInfo(skuInfo);
        return Result.ok();
    }

    @GetMapping("list/{page}/{limit}")
    public Result getSkuInfoPage(@PathVariable Long page,
                                 @PathVariable Long limit){
        Page<SkuInfo> skuInfoPage = new Page<>(page, limit);
        IPage iPage = manageService.getSkuInfoPage(skuInfoPage);
        return Result.ok(iPage);
    }
    /**
     * 上架sku
     * @param skuId
     */
    @GetMapping("/onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId){
        manageService.onSale(skuId);
        return Result.ok();
    }
    /**
     * 下架
     * @param skuId
     */
    @GetMapping("/cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId){
        manageService.cancelSale(skuId);
        return Result.ok();
    }
}

