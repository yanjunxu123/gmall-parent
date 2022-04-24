package com.atguigu.gmall.item.service;

import java.util.Map;

public interface ItemApiService {
    /**
     * 根据skuId获取商品详情信息
     * @param skuId
     * @return
     */
    Map<String, Object> getItem(Long skuId);
}
