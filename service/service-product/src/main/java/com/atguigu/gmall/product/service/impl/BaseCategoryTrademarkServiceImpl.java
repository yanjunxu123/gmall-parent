/**
 * @author yjx
 * @date 2021年 12月14日 16:42:57
 */
package com.atguigu.gmall.product.service.impl;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.mapper.BaseCategoryTrademarkMapper;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BaseCategoryTrademarkServiceImpl extends ServiceImpl<BaseCategoryTrademarkMapper, BaseCategoryTrademark> implements BaseCategoryTrademarkService {
    @Autowired
    private BaseCategoryTrademarkMapper baseCategoryTrademarkMapper;

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;
    /**
     * 保存类别与品牌的关系
     * @param categoryTrademarkVo
     */
    @Override
    public void saveBaseCategoryTrademark(CategoryTrademarkVo categoryTrademarkVo) {
        List<Long> trademarkIdList = categoryTrademarkVo.getTrademarkIdList();
        if (!CollectionUtils.isEmpty(trademarkIdList)){
            List<BaseCategoryTrademark> baseCategoryTrademarkList = trademarkIdList.stream().map(trademarkId -> {
                //创建分类id与品牌的关系对象
                BaseCategoryTrademark baseCategoryTrademark = new BaseCategoryTrademark();
                baseCategoryTrademark.setCategory3Id(categoryTrademarkVo.getCategory3Id());
                baseCategoryTrademark.setTrademarkId(trademarkId);
                return baseCategoryTrademark;
            }).collect(Collectors.toList());
            this.saveBatch(baseCategoryTrademarkList);
        }
    }

    /**
     * 逻辑删除
     * 删除分类与品牌的关系
     * @param category3Id
     * @param trademarkId
     * @return
     */
    @Override
    public void deleteBaseCategoryTrademark(Long category3Id, Long trademarkId) {
        QueryWrapper<BaseCategoryTrademark> baseCategoryTrademarkQueryWrapper = new QueryWrapper<>();
        baseCategoryTrademarkQueryWrapper.eq("category3_id",category3Id);
        baseCategoryTrademarkQueryWrapper.eq("trademark_id",trademarkId);
        baseCategoryTrademarkMapper.delete(baseCategoryTrademarkQueryWrapper);
    }

    /**
     * 根据三级id获取所有品牌列表
     * @param category3Id
     * @return
     */
    @Override
    public List<BaseTrademark> findTrademarkList(Long category3Id) {

        //根据category3_id获取base_category_trademark的数据
        QueryWrapper<BaseCategoryTrademark> baseCategoryTrademarkQueryWrapper = new QueryWrapper<>();
        baseCategoryTrademarkQueryWrapper.eq("category3_id",category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarkList = baseCategoryTrademarkMapper.selectList(baseCategoryTrademarkQueryWrapper);
        if (!CollectionUtils.isEmpty(baseCategoryTrademarkList)){
            //再根据base_category_trademark获取所有的trademark_id
            List<Long> trademarkIdList = baseCategoryTrademarkList.stream().map(baseCategoryTrademark -> {
                return baseCategoryTrademark.getTrademarkId();
            }).collect(Collectors.toList());

            //最后根据trademark_id获取base_trademark的数据
            return baseTrademarkMapper.selectBatchIds(trademarkIdList);
        }
        return null;
    }

    /**
     * 根据三级id获取所有可选择的品牌列表
     * @param category3Id
     * @return
     */
    @Override
    public List<BaseTrademark> findCurrentTrademarkList(Long category3Id) {
        //根据三级分类id得到所有的base_category_trademark数据
        QueryWrapper<BaseCategoryTrademark> baseCategoryTrademarkQueryWrapper = new QueryWrapper<>();
        baseCategoryTrademarkQueryWrapper.eq("category3_id",category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarkList = baseCategoryTrademarkMapper.selectList(baseCategoryTrademarkQueryWrapper);
        //再活动所有的关联的base_trademark的id
        if (!CollectionUtils.isEmpty(baseCategoryTrademarkList)){
            List<Long> trademarkIdList= baseCategoryTrademarkList.stream().map(baseCategoryTrademark -> {
                return baseCategoryTrademark.getTrademarkId();
            }).collect(Collectors.toList());

            //再根据得到的id集合过滤出可选的品牌的id集合
            List<BaseTrademark> baseTrademarkList = baseTrademarkMapper.selectList(null).stream().filter(baseTrademark -> {
                return !trademarkIdList.contains(baseTrademark.getId());
            }).collect(Collectors.toList());

            return baseTrademarkList;
        }
        return baseTrademarkMapper.selectList(null);
    }


}

