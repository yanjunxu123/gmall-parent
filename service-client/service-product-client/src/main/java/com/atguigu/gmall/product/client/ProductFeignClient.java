package com.atguigu.gmall.product.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.impl.ProductDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient(value = "service-product",fallback = ProductDegradeFeignClient.class)
public interface ProductFeignClient {
    /**
     * 根据skuId返回商品价格
     * @param skuId
     * @return
     */
    @GetMapping("/api/product/inner/getSkuPrice/{skuId}")
     BigDecimal getSkuPrice(@PathVariable Long skuId);

    //商品的基本信息 skuInfo + skuImageList
    /**
     * 根据skuId查询skuInfo同时查询skuImag信息 放入skuInfo中并返回
     * @param skuId
     * @return
     */
    @GetMapping("/api/product/inner/getSkuInfo/{skuId}")
     SkuInfo getSkuInfo(@PathVariable Long skuId);
    /**
     *  分类信息数据
     * 通过category3Id获取分类信息（获取视图的方式）
     * @param category3Id
     * @return
     */
    @GetMapping("/api/product/inner/getCategoryView/{category3Id}")
     BaseCategoryView getCategoryView(@PathVariable Long category3Id);
    /*
        回显销售属性 + 销售属性值 + 锁定！

   */
    @GetMapping("/api/product/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId,
                                                          @PathVariable Long spuId);


    @GetMapping("/api/product/inner/getSkuValueIdsMap/{spuId}")
    Map getSkuValueIdsMap(@PathVariable("spuId") Long spuId);

    @GetMapping("/api/product/inner/findSpuPosterBySpuId/{spuId}")
    public List<SpuPoster> findSpuPosterBySpuId(@PathVariable Long spuId);

    @GetMapping("/api/product/inner/getAttrList/{skuId}")
    List<BaseAttrInfo> getAttrList(@PathVariable Long skuId);

    @GetMapping("/api/product/inner/getBaseCategoryList")
    Result getBaseCategoryList();

    @GetMapping("/api/product/inner/getTrademark/{tmId}")
    BaseTrademark getTrademarkByTmId(@PathVariable Long tmId);
}
