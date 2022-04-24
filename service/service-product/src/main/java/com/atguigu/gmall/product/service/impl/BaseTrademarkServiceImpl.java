/**
 * @author yjx
 * @date 2021年 12月14日 13:21:21
 */
package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * ServiceImpl中
 *
 *     @Autowired
 *     protected M baseMapper;
 *     protected Class<T> entityClass = this.currentModelClass();
 *     protected Class<T> mapperClass = this.currentMapperClass();
 */
@Service
public class BaseTrademarkServiceImpl extends ServiceImpl<BaseTrademarkMapper,BaseTrademark> implements BaseTrademarkService {
    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    /**
     * 品牌分页
     * @param baseTrademarkPage
     * @return
     */
    @Override
    public IPage getBaseTrademarkPage(Page<BaseTrademark> baseTrademarkPage) {
        QueryWrapper<BaseTrademark> baseTrademarkQueryWrapper = new QueryWrapper<>();
        baseTrademarkQueryWrapper.orderByDesc("id");
        return baseTrademarkMapper.selectPage(baseTrademarkPage,baseTrademarkQueryWrapper);
    }
}

