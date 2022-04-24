package com.atguigu.gmall.product.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


public interface ManageService {

    List<BaseCategory1> getCategory1();

    List<BaseCategory2> getCategory2(Long category1Id);

    List<BaseCategory3> getCategory3(Long category2Id);

    List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id);

    BaseAttrInfo getAttrInfo(Long attrId);

    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    IPage getSpuInfoPage(Page<SpuInfo> spuInfoPage, SpuInfo spuInfo);

    List<BaseSaleAttr> getBaseSaleAttrList();

    void saveSpuInfo(SpuInfo spuInfo);

    List<SpuImage> getSpuImageList(Long spuId);

    List<SpuSaleAttr> getSpuSaleAttrList(Long spuId);

    void saveSkuInfo(SkuInfo skuInfo);

    IPage getSkuInfoPage(Page<SkuInfo> skuInfoPage);

    void onSale(Long skuId);

    void cancelSale(Long skuId);

    BigDecimal getSkuPrice(Long skuId);

    SkuInfo getSkuInfo(Long skuId);

    /**
     * 通过category3Id获取分类信息（获取视图的方式）
     * @param category3Id
     * @return
     */
    BaseCategoryView getCategoryViewByCategory3Id(Long category3Id);

    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId);

    /**
     * 通过supId获取 组成的valueId 与 skuId 生成一个Json 字符串！
     * @param spuId
     * @return
     */
    Map getSkuValueIdsMap(Long spuId);

    List<SpuPoster> findSpuPosterBySpuId(Long spuId);

    /**
     * 通过skuId 集合来查询数据
     * @param skuId
     * @return
     */
    List<BaseAttrInfo> getAttrList(Long skuId);

    /**
     * 获取全部分类信息
     * @return
     */
    List<JSONObject> getBaseCategoryList();

    /**
     * 通过品牌id获取品牌
     * @param tmId
     * @return
     */
    BaseTrademark getTrademarkByTmId(Long tmId);
}
