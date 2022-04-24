/**
 * @author yjx
 * @date 2021年 12月17日 16:35:47
 */
package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.service.ItemApiService;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


/*
        调用Service-Product中的接口获取数据，对数据进行汇总
 */
@Service
public class ItemApiServiceImpl implements ItemApiService {

    //远程接口调用
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private  ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ListFeignClient listFeignClient;
    /**
     * 根据skuId获取商品详情页的信息。
     * @param skuId
     * @return
     */
    @Override
    public Map<String, Object> getItem(Long skuId) {
        HashMap<String, Object> result = new HashMap<>();
        CompletableFuture<SkuInfo> skuCompletableFuture = CompletableFuture.supplyAsync(() -> {
            //  获取到的数据是skuInfo + skuImageList
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            result.put("skuInfo", skuInfo);
            return skuInfo;
        }, threadPoolExecutor);
        //获取分类信息
        CompletableFuture<Void> categoryViewCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            result.put("categoryView", categoryView);
        },threadPoolExecutor);
        //获取商品最新价格
        CompletableFuture<Void> skuPriceCompletableFuture  = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            result.put("price", skuPrice);
        }, threadPoolExecutor);
        //  获取销售属性+销售属性值
        CompletableFuture<Void> spuSaleAttrCompletableFuture  = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
            List<SpuSaleAttr> spuSaleAttrListCheckBySku = productFeignClient.getSpuSaleAttrListCheckBySku(skuInfo.getId(),skuInfo.getSpuId());
            result.put("spuSaleAttrList", spuSaleAttrListCheckBySku);
        }, threadPoolExecutor);


        //  查询销售属性值Id 与skuId 组合的map
        CompletableFuture<Void> skuValueIdsMapCompletableFuture  = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
            Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            //Map转换为json字符串
            String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);
            result.put("valuesSkuJson", valuesSkuJson);
        }, threadPoolExecutor);

        //  获取海报数据
        CompletableFuture<Void> spuPosterListCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
            List<SpuPoster> spuPosterList = productFeignClient.findSpuPosterBySpuId(skuInfo.getSpuId());
            result.put("spuPosterList", spuPosterList);
        }, threadPoolExecutor);

        //  获取sku平台属性，即规格数据
        CompletableFuture<Void> skuAttrListCompletableFuture  = CompletableFuture.runAsync(() -> {
            List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
            List<Map<String, Object>> skuAttrList  = attrList.stream().map(baseAttrInfo -> {
                Map<String, Object> map = new HashMap<>();
                map.put("attrName", baseAttrInfo.getAttrName());
                map.put("attrValue", baseAttrInfo.getAttrValueList().get(0).getValueName());
                return map;
            }).collect(Collectors.toList());
            result.put("skuAttrList",skuAttrList);
        }, threadPoolExecutor);

        //更新商品incrHotScore
        CompletableFuture<Void> incrHotScoreCompletableFuture = CompletableFuture.runAsync(() -> {
            listFeignClient.incrHotScore(skuId);
        }, threadPoolExecutor);

        CompletableFuture.allOf(
                skuCompletableFuture,
                categoryViewCompletableFuture,
                skuPriceCompletableFuture,
                spuSaleAttrCompletableFuture,
                skuValueIdsMapCompletableFuture,
                spuPosterListCompletableFuture,
                skuAttrListCompletableFuture,
                incrHotScoreCompletableFuture
        ).join();
        return result;
    }

//    @Override
//    public Map<String, Object> getItem(Long skuId) {
//        HashMap<String, Object> result = new HashMap<>();
//
//
//        CompletableFuture<SkuInfo> skuCompletableFuture = CompletableFuture.supplyAsync(() -> {
//            //  获取到的数据是skuInfo + skuImageList
//            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
//            result.put("skuInfo", skuInfo);
//            return skuInfo;
//        }, threadPoolExecutor);
//        //  获取到的数据是skuInfo + skuImageList
//        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
//
//        //  判断skuInfo 不为空
//
//        if (skuInfo != null) {
//            //  获取分类数据
//            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
//            result.put("categoryView",categoryView);
//            //  获取销售属性+销售属性值
//            List<SpuSaleAttr> spuSaleAttrListCheckBySku = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
//            result.put("spuSaleAttrList",spuSaleAttrListCheckBySku);
//            //  查询销售属性值Id 与skuId 组合的map
//            Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
//            //将Map转换为json字符串
//            String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);
//            result.put("valuesSkuJson",valuesSkuJson);
//        }
//        //  获取价格
//        BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
//        //  map 中 key 对应的谁? Thymeleaf 获取数据的时候 ${skuInfo.skuName}
//        result.put("price",skuPrice);
//
//        //  返回map 集合 Thymeleaf 渲染：能用map 存储数据！
//
//        //  spu海报数据
//        List<SpuPoster> spuPosterList = productFeignClient.findSpuPosterBySpuId(skuInfo.getSpuId());
//        result.put("spuPosterList",spuPosterList);
//
//        //规格参数
//        List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
//        List<Map<String, String>> skuAttrList = attrList.stream().map(baseAttrInfo -> {
//            Map<String, String> stringStringHashMap = new HashMap<>();
//            stringStringHashMap.put("attrName", baseAttrInfo.getAttrName());
//            stringStringHashMap.put("attrValue", baseAttrInfo.getAttrValueList().get(0).getValueName());
//            return stringStringHashMap;
//        }).collect(Collectors.toList());
//        result.put("skuAttrList",skuAttrList);
//        return result;
//    }
}

