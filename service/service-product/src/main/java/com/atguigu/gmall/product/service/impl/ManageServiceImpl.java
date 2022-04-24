/**
 * @author yjx
 * @date 2021年 12月11日 17:23:20
 */
package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ManageServiceImpl implements ManageService {
    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuPosterMapper spuPosterMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Autowired
    private RabbitService rabbitService;
    /**
     * 上架sku
     * @param skuId
     */
    @Override
    @Transactional
    public void onSale(Long skuId) {
        SkuInfo skuInfoUp = new SkuInfo();
        skuInfoUp.setId(skuId);
        skuInfoUp.setIsSale(1);
        skuInfoMapper.updateById(skuInfoUp);

        //用消息中间件发送商品上架的消息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_UPPER,skuId);
    }

    /**
     * 下架
     * @param skuId
     */
    @Override
    @Transactional
    public void cancelSale(Long skuId) {
        SkuInfo skuInfoUp = new SkuInfo();
        skuInfoUp.setId(skuId);
        skuInfoUp.setIsSale(0);
        skuInfoMapper.updateById(skuInfoUp);
        //用消息中间件发送商品下架的消息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_LOWER,skuId);
    }

    /**
     * 查询一级分类
     * @return
     */
    @Override
    public List<BaseCategory1> getCategory1() {
        return baseCategory1Mapper.selectList(null);
    }

    /**
     * 根据一级分类id，查找二级分类信息
     * @param category1Id
     * @return
     */
    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        return baseCategory2Mapper.selectList(new QueryWrapper<BaseCategory2>().eq("category1_id",category1Id));
    }

    /**
     * 根据二级分类id，查找三级分类信息
     * @param category2Id
     * @return
     */
    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        return baseCategory3Mapper.selectList(new QueryWrapper<BaseCategory3>().eq("category2_id",category2Id));
    }

    /**
     *根据分类id查找平台属性
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        return baseAttrInfoMapper.selectAttrInfoList(category1Id,category2Id,category3Id);
    }

    /**
     * 通过平台属性id得到平台属性
     * @param attrId
     * @return
     */
    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        if (baseAttrInfo !=null){
            baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        }
        return baseAttrInfo;
    }

    /**
     * 通过平台属性id得到平台属性，从而得到平台属性值
     * @param attrId
     * @return
     */
    private List<BaseAttrValue> getAttrValueList(Long attrId) {
        QueryWrapper<BaseAttrValue> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("attr_id",attrId);
        return baseAttrValueMapper.selectList(queryWrapper);
    }

    /**
     * 平台属性的修改、保存（新增）。 对平台属性的dml 同时要兼顾平台属性值
     * @param baseAttrInfo
     */
    @Transactional
    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        if (baseAttrInfo.getId() != null) {
            //有平台属性id，说明应该进行更新操作
            baseAttrInfoMapper.updateById(baseAttrInfo);
        }else {
            //没有得到平台属性id，说明应该进行插入操作
           baseAttrInfoMapper.insert(baseAttrInfo);
        }


        //无论进行更新还是插入平台属性（baseAttrInfo），都进行一次逻辑删除并插入平台属性值（baseAttrValue）
        baseAttrValueMapper.delete(new QueryWrapper<BaseAttrValue>().eq("attr_id",baseAttrInfo.getId()));

        //插入baseAttrValue将baseAttrInfo中的平台属性值赋值给 baseAttrValue
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (attrValueList.size() > 0 && attrValueList != null) {
            for (BaseAttrValue baseAttrValue : attrValueList) {
                //获取平台属性给平台属性值
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(baseAttrValue);
            }
        }
    }

    /**
     * SPU的分页列表
     * @param spuInfoPage
     * @param spuInfo
     * @return
     */
    @Override
    public IPage getSpuInfoPage(Page<SpuInfo> spuInfoPage, SpuInfo spuInfo) {
        QueryWrapper<SpuInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category3_id",spuInfo.getCategory3Id());
        queryWrapper.orderByDesc("id");
        return spuInfoMapper.selectPage(spuInfoPage,queryWrapper);
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectList(null);
    }

    /**
     * 保存spu相关信息
     * @param spuInfo
     */
    @Override
    @Transactional(rollbackFor = Exception.class) //多张表操作加事务
    public void saveSpuInfo(SpuInfo spuInfo) {
        //保存 spu信息
        spuInfoMapper.insert(spuInfo);
        //保存 商品图片
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (!CollectionUtils.isEmpty(spuImageList)){
            spuImageList.forEach(spuImage -> {
                //spu_id 从spu_Info 中获取。
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            });
        }
        //保存商品海报
        List<SpuPoster> spuPosterList = spuInfo.getSpuPosterList();
        if (!CollectionUtils.isEmpty(spuPosterList)) {
            spuPosterList.forEach(spuPoster -> {
               spuPoster.setSpuId(spuInfo.getId());
               spuPosterMapper.insert(spuPoster);
            });
        }
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (!CollectionUtils.isEmpty(spuSaleAttrList)) {
            spuSaleAttrList.forEach(spuSaleAttr -> {
               spuSaleAttr.setSpuId(spuInfo.getId());
               spuSaleAttrMapper.insert(spuSaleAttr);

                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (!CollectionUtils.isEmpty(spuSaleAttrValueList)) {
                    spuSaleAttrValueList.forEach(spuSaleAttrValue -> {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    });
                }
            });
        }
    }

    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        QueryWrapper<SpuImage> spuImageQueryWrapper = new QueryWrapper<>();
        spuImageQueryWrapper.eq("spu_id",spuId);
       return spuImageMapper.selectList(spuImageQueryWrapper);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(Long spuId) {
       return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    /**
     * 保存sku信息
     * @param skuInfo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSkuInfo(SkuInfo skuInfo) {
        skuInfoMapper.insert(skuInfo);
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)){
//            for (SkuImage skuImage : skuImageList) {
//                skuImage.setSkuId(skuInfo.getSpuId());
//                skuImageMapper.insert(skuImage);
//            }
            skuImageList.forEach(skuImage -> {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            });
        }

        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)) {
            skuSaleAttrValueList.forEach(skuSaleAttrValue -> {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            });
        }

        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)) {
            skuAttrValueList.forEach(skuAttrValue -> {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            });
        }
    }

    /**
     * spu分页
     * @param skuInfoPage
     * @return
     */
    @Override
    public IPage getSkuInfoPage(Page<SkuInfo> skuInfoPage) {

        QueryWrapper<SkuInfo> skuInfoQueryWrapper = new QueryWrapper<>();
        skuInfoQueryWrapper.orderByDesc("id");

        IPage<SkuInfo> iPage = skuInfoMapper.selectPage(skuInfoPage, skuInfoQueryWrapper);

        return iPage;
    }


    /**
     * 通过skuId获取商品详情页中的商品价格
     * @param skuId
     * @return
     */
    @Override
    public BigDecimal getSkuPrice(Long skuId) {
        BigDecimal price = null;
        //直接访问数据库应该加锁，保护数据库
        RLock lock = redissonClient.getLock(skuId + ":lock");
        lock.lock();
        SkuInfo skuInfo = null;
        price = new BigDecimal(0);
        try {
            QueryWrapper<SkuInfo> skuInfoQueryWrapper = new QueryWrapper<>();
            skuInfoQueryWrapper.eq("id",skuId);
            skuInfoQueryWrapper.select("price");
            skuInfo = skuInfoMapper.selectById(skuId);
            if (skuInfo != null) {
                price =skuInfo.getPrice();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            //解锁
            lock.unlock();
        }
        return price;
    }

    /**
     * 根据skuId查询sku的信息和查找相关的skuImage的信息赋值给skuInfo并返回
     * @param skuId
     * @return
     */
    @Override
    @GmallCache(prefix = RedisConst.SKUKEY_PREFIX)
    public SkuInfo getSkuInfo(Long skuId) {
        return getSkuInfoDB(skuId);
    }

//    private SkuInfo getSkuInfoRedisson(Long skuId) {
//        SkuInfo skuInfo = null;
//        // 缓存数据
//        // 创建一个key
//        String skukey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKUKEY_SUFFIX;
//        try {
//            // 缓存数据
//             skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skukey);
//            //判断缓存中是否有数据
//            if (skuInfo == null) {
//                //为防止数据库被击穿，应该加锁
//                //建一个锁的key‘
//                String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
//                //RedisSon创建锁
//                //RedisSon锁的类型：
//                //             第一种： lock.lock();
//                //            第二种:  lock.lock(10,TimeUnit.SECONDS);
//                //            第三种： lock.tryLock(100,10,TimeUnit.SECONDS);
//
//                RLock lock = redissonClient.getLock(lockKey);
//                boolean res = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
//                if (res) {
//                    try {
//                        //得到锁，进行业务
//                        skuInfo = getSkuInfoDB(skuId);
//                        if (skuInfo == null) {
//                            //为防止缓存穿透，当数据库中无数据时 给缓存一个空的数据
//                            //创建一个新对象用于存空数据到缓存中
//                            SkuInfo skuInfo1 = new SkuInfo();
//                            redisTemplate.opsForValue().set(skukey,skuInfo1,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
//                            return skuInfo1;
//                        }
//                        //查询数据库，有值，同时将数据放入缓存
//                        redisTemplate.opsForValue().set(skukey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
//                        return skuInfo;
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    } finally {
//                        lock.unlock();
//                    }
//                }else {//没有得到锁，进行重试
//                    Thread.sleep(1000);
//                    return getSkuInfo(skuId);
//                }
//            }else {
//                //缓存中有数据
//                return skuInfo;
//            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        //最后防止缓存宕机，从数据库查数据兜底
//        return getSkuInfoDB(skuId);
//    }

    private SkuInfo getSkuInfoDB(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        QueryWrapper<SkuImage> skuImageQueryWrapper = new QueryWrapper<>();
        skuImageQueryWrapper.eq("sku_id",skuId);
        List<SkuImage> skuImages = skuImageMapper.selectList(skuImageQueryWrapper);
        skuInfo.setSkuImageList(skuImages);
        return skuInfo;
    }

    /**
     * 通过category3Id获取分类信息（获取视图的方式）
     * @param category3Id
     * @return
     */
    @Override
    @GmallCache(prefix = "CategoryViewByCategory3Id:")
    public BaseCategoryView getCategoryViewByCategory3Id(Long category3Id) {
       return baseCategoryViewMapper.selectById(category3Id);
    }

    @Override
    @GmallCache(prefix = "SaleAttrListCheckBySku:")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId,Long spuId) {

        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId,spuId);
    }

    @Override
    @GmallCache(prefix = "SaleAttrValuesBySpu:")
    public Map getSkuValueIdsMap(Long spuId) {
        Map<Object, Object> map = new HashMap<>();
        // key = 125|123 ,value = 37
        List<Map> mapList = skuSaleAttrValueMapper.selectSaleAttrValuesBySpu(spuId);
        if (mapList != null && mapList.size() > 0) {
            // 循环遍历
            for (Map skuMap : mapList) {
                // key = 125|123 ,value = 37
                map.put(skuMap.get("value_ids"), skuMap.get("sku_id"));
            }
        }
        return map;

    }

    @Override
    @GmallCache(prefix = "SpuPosterList:")
    public List<SpuPoster> findSpuPosterBySpuId(Long spuId) {
        QueryWrapper<SpuPoster> spuPosterQueryWrapper = new QueryWrapper<>();
        spuPosterQueryWrapper.eq("spu_id",spuId);
        return  spuPosterMapper.selectList(spuPosterQueryWrapper);
    }

    @Override
    @GmallCache(prefix = "baseAttrInfoList:")
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        return baseAttrInfoMapper.selectAttrInfoListBySkuId(skuId);
    }



    @Override
    @GmallCache(prefix = "category:")
    public List<JSONObject> getBaseCategoryList() {
        //一个保存json的集合
        ArrayList<JSONObject> list = new ArrayList<>();
        //先获取所用的分类信息。
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        //先根据一级分类id来分组,Map中所存储的键值对的键值是一级分类id，值是所对应的BaseCategoryView
        Map<Long, List<BaseCategoryView>> category1Map = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        //声明一个变量
        int index = 1;
        for (Map.Entry<Long, List<BaseCategoryView>> entry : category1Map.entrySet()) {
            //取得一级分类id
            Long category1Id = entry.getKey();
            //一级分类id所对应的数据集合
            List<BaseCategoryView> categoryViewList2 = entry.getValue();

            //声明一个对象，存储一级分类的数据
            JSONObject category1 = new JSONObject();
            category1.put("index",index);
            category1.put("categoryName",categoryViewList2.get(0).getCategory1Name());
            category1.put("categoryId",category1Id);
            //变量index自增
            index++;
            //以二级分类Id 为基准进行分组,获取二级分类信息。
            Map<Long, List<BaseCategoryView>> category2Map = categoryViewList2.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            //用于存储二级分类信息的集合
            List<JSONObject> category2Child = new ArrayList<>();
            for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
                //  二级分类id
                Long category2Id = entry2.getKey();
                //获取二级分类下的集合数据
                List<BaseCategoryView> categoryViewList3 = entry2.getValue();
                //新建一个用于存储二级分类信息的对象
                JSONObject category2 = new JSONObject();
                category2.put("categoryId",category2Id);
                category2.put("categoryName",categoryViewList3.get(0).getCategory2Name());
                category2Child.add(category2);

                //用于存储三级分类信息的集合
                List<JSONObject> category3Child = new ArrayList<>();
                //遍历三级分类集合获取数据
                categoryViewList3.stream().forEach(category3View ->{
                    //创建存储3级分类信息的对象
                    JSONObject category3 = new JSONObject();
                    category3.put("categoryId",category3View.getCategory3Id());
                    category3.put("categoryName",category3View.getCategory3Name());
                    category3Child.add(category3);
                });
                //三级分类信息放入二级分类中
                category2.put("categoryChild",category3Child);
            }
//            while (entry2.hasNext()) {
//                //二级分类id
//                Long category2Id = entry2.next().getKey();
//                //获取二级分类下的集合数据
//                List<BaseCategoryView> categoryViewList3 = entry2.next().getValue();
//                //新建一个用于存储二级分类信息的对象
//                JSONObject category2 = new JSONObject();
//                category2.put("categoryId",category2Id);
//                category2.put("categoryName",categoryViewList3.get(0).getCategory2Name());
//                category2Child.add(category2);
//
//                //用于存储三级分类信息的集合
//                List<JSONObject> category3Child = new ArrayList<>();
//                //遍历三级分类集合获取数据
//                categoryViewList3.stream().forEach(category3View ->{
//                    //创建存储3级分类信息的对象
//                    JSONObject category3 = new JSONObject();
//                    category3.put("categoryId",category3View.getCategory3Id());
//                    category3.put("categoryName",category3View.getCategory3Name());
//                    category3Child.add(category3);
//                });
//                //三级分类信息放入二级分类中
//                category2.put("categoryChild",category3Child);
//            }

            //将二级分类信息放入一级分类中
            category1.put("categoryChild",category2Child);
            list.add(category1);
        }
        return list;
    }

    /**
     * 通过品牌Id 集合来查询数据
     * @param tmId
     * @return
     */
    @Override
    public BaseTrademark getTrademarkByTmId(Long tmId) {
        return baseTrademarkMapper.selectById(tmId);
    }
}

