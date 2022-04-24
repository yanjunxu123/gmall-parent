/**
 * @author yjx
 * @date 2021年 12月11日 17:15:50
 */
package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/product")
public class BaseManageController {

    @Autowired
    private ManageService manageService;


    /**
     * 查询一级分类
     * @return
     */
    @GetMapping("getCategory1")
    public Result<List<BaseCategory1>> getCategory1(){
       List<BaseCategory1> baseCategory1List = manageService.getCategory1();
        return Result.ok(baseCategory1List);
    }

    /**
     * 根据一级分类id，查找二级分类信息
     * @param category1Id
     * @return
     */
    @GetMapping("/getCategory2/{category1Id}")
    public Result<List<BaseCategory2>> getCategory2(@PathVariable() Long category1Id) {
        return Result.ok(manageService.getCategory2(category1Id));
    }

    /**
     * 根据二级分类id，查找三级分类信息
     * @param category2Id
     * @return
     */
    @GetMapping("/getCategory3/{category2Id}")
    public Result<List<BaseCategory3>> getCategory3(@PathVariable() Long category2Id){
        return Result.ok(manageService.getCategory3(category2Id));
    }

    /**
     * 根据分类id查找平台属性
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    @GetMapping("/attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result<List<BaseAttrInfo>> getAttrInfoList(
            @PathVariable("category1Id") Long category1Id,
             @PathVariable("category2Id") Long category2Id,
            @PathVariable("category3Id") Long category3Id){
        return Result.ok(manageService.getAttrInfoList(category1Id, category2Id,category3Id));
    }

    /**
     * 回显商品属性值
     * @param attrId
     * @return
     */
    @GetMapping("/getAttrValueList/{attrId}")
    public Result getAttrValueList(@PathVariable("attrId") Long attrId ){
        BaseAttrInfo baseAttrInfo = manageService.getAttrInfo(attrId);
        return Result.ok(baseAttrInfo.getAttrValueList());
    }

    /**
     * 保存（新增）平台属性
     * @param baseAttrInfo 平台属性
     * @return
     */
    @PostMapping("/saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        manageService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }


}

