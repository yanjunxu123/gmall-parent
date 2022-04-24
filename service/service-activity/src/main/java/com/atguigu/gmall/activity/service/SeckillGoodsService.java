package com.atguigu.gmall.activity.service;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;

import java.util.List;

public interface SeckillGoodsService {
    /**
     * 查询秒杀列表
     * @return
     */
    List<SeckillGoods> findAll();

    /**
     * 获取秒杀商品的实体
     * @param skuId
     * @return
     */
    SeckillGoods getSeckillGoods(Long skuId);

    /**
     * 用户预下单
     * @param userId
     * @param skuId
     */
    void seckillOrder(String userId, Long skuId);

    Result checkOrder(Long skuId, String userId);
}
