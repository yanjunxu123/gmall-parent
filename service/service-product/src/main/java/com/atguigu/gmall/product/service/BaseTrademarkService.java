package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 使用IService接口
 * 作用：业务逻辑层的接口，也是用于定义业务层的访问接口
 *
 *  default boolean save(T entity) {
 *         return SqlHelper.retBool(this.getBaseMapper().insert(entity));
 *     }
 */

public interface BaseTrademarkService extends IService<BaseTrademark> {
    IPage getBaseTrademarkPage(Page<BaseTrademark> baseTrademarkPage);
}
