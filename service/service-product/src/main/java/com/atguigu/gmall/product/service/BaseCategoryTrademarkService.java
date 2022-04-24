package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BaseCategoryTrademarkService extends IService<BaseCategoryTrademark> {
    /**
     * 保存三级分类id与品牌的关系
     * @param categoryTrademarkVo
     */
    void saveBaseCategoryTrademark(CategoryTrademarkVo categoryTrademarkVo);

    /**
     * 删除三级分类与品牌的关系
     * @param category3Id
     * @param trademarkId
     */
    void deleteBaseCategoryTrademark(Long category3Id, Long trademarkId);

    /**
     * 通过三级分类id获取所有品牌列表
     * @param category3Id
     * @return
     */
    List<BaseTrademark> findTrademarkList(Long category3Id);

    /**
     * 通过三级分类id获取可选择的品牌
     * @param category3Id
     * @return
     */
    List<BaseTrademark> findCurrentTrademarkList(Long category3Id);
}
