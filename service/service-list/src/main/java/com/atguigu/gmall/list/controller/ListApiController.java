/**
 * @author yjx
 * @date 2021年 12月22日 18:31:54
 */
package com.atguigu.gmall.list.controller;

import com.atguigu.gmall.common.result.Result;

import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.Goods;

import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("api/list")
public class ListApiController {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private SearchService searchService;

    /**
     * 创建索引与映射
     * @return
     */
    @GetMapping("inner/createIndex")
    public Result createIndex(){
        //创建索引
        restTemplate.createIndex(Goods.class);
        //创建映射
        restTemplate.putMapping(Goods.class);
        return Result.ok();
    }

    /**
     * 上架商品
     * @param skuId
     * @return
     */
    @GetMapping("inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable Long skuId){
        searchService.upperGoods(skuId);
        return Result.ok();
    }

    /**
     * 下架商品
     * @param skuId
     * @return
     */
    @GetMapping("inner/lowerGoods/{skuId}")
    public Result lowerGoods(@PathVariable Long skuId){
        searchService.lowerGoods(skuId);
        return Result.ok();
    }

    @GetMapping("inner/incrHotScore/{skuId}")
    public Result incrHotScore(@PathVariable Long skuId){
        searchService.incrHotScore(skuId);
        return Result.ok();
    }

    @PostMapping
    public Result searchList(@RequestBody SearchParam searchParam) throws IOException {
        SearchResponseVo response  = searchService.search(searchParam);
        return Result.ok(response);
    }

}

