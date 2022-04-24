/**
 * @author yjx
 * @date 2022年 01月14日 19:14:24
 */
package com.atguigu.gmall.activity.service.impl;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.activity.util.CacheHelper;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    /**
     * 查询秒杀商品列表
     * @return
     */
    @Override
    public List<SeckillGoods> findAll() {
        List<SeckillGoods> list = redisTemplate.opsForHash().values(RedisConst.SECKILL_GOODS);
        return list;
    }

    /**
     * 获取秒杀商品的实体
     * @param skuId
     * @return
     */
    @Override
    public SeckillGoods getSeckillGoods(Long skuId) {
        return (SeckillGoods) redisTemplate.opsForHash().get(RedisConst.SECKILL_GOODS,skuId.toString());
    }

    @Override
    public void seckillOrder(String userId, Long skuId) {
        //1.先判断状态位，因为或许这时已经商品被秒杀完了
        if (StringUtils.isEmpty(CacheHelper.get(skuId.toString())) || "0".equals(CacheHelper.get(skuId.toString()))) {
            return;
        }
        //2.判断用户是否下过单'
        //   key  = seckill:user:userId
        String userKey = RedisConst.SECKILL_USER+userId;
        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(userKey, skuId.toString(), RedisConst.SECKILL__TIMEOUT, TimeUnit.SECONDS);
        if (!aBoolean){
            //用户下过单了
            return;
        }
        //3.  获取库存！
        String stockSkuId = (String) redisTemplate.opsForList().rightPop(RedisConst.SECKILL_STOCK_PREFIX + skuId);
        if (StringUtils.isEmpty(stockSkuId)) {
            //  通知其他兄弟节点，该商品已经售罄！
            redisTemplate.convertAndSend("seckillpush",skuId+":0");
            return;
        }
        OrderRecode orderRecode = new OrderRecode();
        orderRecode.setNum(1);
        orderRecode.setUserId(userId);
        orderRecode.setSeckillGoods(this.getSeckillGoods(skuId));
        orderRecode.setOrderStr(MD5.encrypt(userId+skuId));

        //  确定数据类型！ hash key = seckill:orders  field = userId  value = orderRecode
        this.redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).put(userId,orderRecode);
        //  更新库存！ redis ， mysql！
        this.updateStockCount(skuId);
    }

    @Override
    public Result checkOrder(Long skuId, String userId) {
          /*
        1.  先判断用户是否存在！
        2.  判断用户是否抢购成功！
        3.  判断用户是否下过订单！
        4.  判断状态位！
         */
        //  先判断用户是否存在 看缓存中是否有用户对应的key！
        Boolean aBoolean = redisTemplate.hasKey(RedisConst.SECKILL_USER + userId);
        if (aBoolean) {
            //  只有用存在了，才能判断用户是否抢购成功！ 判断用户是否有预下单记录！
            Boolean result = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).hasKey(userId);
            if (result) { //  result = true 说明用户有预下单记录，则抢购成功！ 去下单！
                //  获取订单记录
                OrderRecode orderRecode = (OrderRecode) this.redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
                //  返回数据
                return Result.build(orderRecode, ResultCodeEnum.SECKILL_SUCCESS);
            }
        }
        //  判断用户是否下过订单！ 意味着在缓存中有真正的订单记录，同时在数据库中也有记录！
        //  存储真正下单的数据类型  Hash  key = RedisConst.SECKILL_ORDERS_USERS field = userId  value = orderId
        Boolean exist = this.redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).hasKey(userId);
        //  exist = true； 说明用户已经下过订单了。 提示抢购成功，去看我的订单
        if (exist){
            //  获取订单Id
            String orderId = (String) this.redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).get(userId);
            //  返回数据
            return Result.build(orderId, ResultCodeEnum.SECKILL_ORDER_SUCCESS);
        }

        //  校验 状态位！ skuId:status  status 0 or 1
        String status = (String) CacheHelper.get(skuId.toString());
        if(StringUtils.isEmpty(status) || "0".equals(status)){
            //  返回数据
            return Result.build(null, ResultCodeEnum.SECKILL_FINISH);
        }
        //  默认排队中
        return Result.build(null, ResultCodeEnum.SECKILL_RUN);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStockCount(Long skuId) {
        RLock lock = redissonClient.getLock("lock:" + skuId);
        lock.lock();
        try {
            Long size = redisTemplate.opsForList().size(RedisConst.SECKILL_STOCK_PREFIX + skuId);
            if (size%2==0){ //  为了避免频繁更新mysql ，redis！
                //  更新数据！
                SeckillGoods seckillGoods = new SeckillGoods();
                seckillGoods.setStockCount(size.intValue());
                QueryWrapper<SeckillGoods> seckillGoodsQueryWrapper = new QueryWrapper<>();
                seckillGoodsQueryWrapper.eq("sku_id",skuId);
                this.seckillGoodsMapper.update(seckillGoods,seckillGoodsQueryWrapper);

                //  更新redis！
                //  hget key field  hset key field value
                SeckillGoods seckillGoodsUpd = this.getSeckillGoods(skuId);
                seckillGoodsUpd.setStockCount(size.intValue());

                this.redisTemplate.opsForHash().put(RedisConst.SECKILL_GOODS,skuId.toString(),seckillGoodsUpd);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
    }
}

