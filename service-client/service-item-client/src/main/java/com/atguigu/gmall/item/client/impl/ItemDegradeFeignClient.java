package com.atguigu.gmall.item.client.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.metadata.IIOMetadataController;

@Component
public class ItemDegradeFeignClient implements ItemFeignClient {

    @Override
    public Result getItem(Long skuId) {
        return  Result.fail();
    }
}
