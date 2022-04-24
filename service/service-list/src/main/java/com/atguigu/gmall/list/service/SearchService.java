package com.atguigu.gmall.list.service;



import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;

import java.io.IOException;


public interface SearchService {
    /**
     * 上架商品
     * @param skuId
     */
    void upperGoods(Long skuId);

    /**
     * 下架商品
     * @param skuId
     */
    void lowerGoods(Long skuId);

    /**
     * 热度排行
     * @param skuId
     */
    void incrHotScore(Long skuId);

    /**
     * 搜索列表
     * @param searchParam
     * @return
     */
    SearchResponseVo search(SearchParam searchParam) throws IOException;


}
