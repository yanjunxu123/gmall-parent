/**
 * 秒杀监听类
 *
 * @author yjx
 * @date 2022年 01月11日 18:33:56
 */
package com.atguigu.gmall.activity.receiver;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

@Component
public class SeckillReceiver {
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SeckillGoodsService seckillGoodsService;
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_1,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_1}
        )
    )
    public void importToRedis(String msg, Message message, Channel channel){

        try {
            //先查数据库找到要加入缓存的秒杀商品，
            QueryWrapper<SeckillGoods> seckillGoodsQueryWrapper = new QueryWrapper<>();
            seckillGoodsQueryWrapper.eq("status",1).gt("stock_count",0);//数量大于0的
            seckillGoodsQueryWrapper.eq("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));
            List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(seckillGoodsQueryWrapper);
            if (!CollectionUtils.isEmpty(seckillGoodsList)){

                for (SeckillGoods seckillGoods : seckillGoodsList) {
                    //用hash 存秒杀商品
                    Boolean aBoolean = redisTemplate.opsForHash().hasKey(RedisConst.SECKILL_GOODS, seckillGoods.getSkuId().toString());
                    if (aBoolean) {
                        //存在的时候，跳过该商品，判断下一个商品
                        continue;
                    }
                    //没有就放进缓存
                    redisTemplate.opsForHash().put(RedisConst.SECKILL_GOODS,seckillGoods.getSkuId().toString(),seckillGoods);
                    //下一步将商品的数量也放入缓存，采用list数据类型。

                    for (Integer i = 0; i <seckillGoods.getStockCount(); i++) {
                        //利用redis中list数据类型具有原子性。
                        redisTemplate.opsForList().leftPush(RedisConst.SECKILL_STOCK_PREFIX+seckillGoods.getSkuId(),seckillGoods.getSkuId().toString());
                    }
                    //  秒杀商品在初始化的时候：状态位初始化 1
                    //  publish seckillpush 46:1  | 后续业务如果说商品被秒杀完了！ publish seckillpush 46:0
                    //            TODO
                    //redis的订阅发布模式，告知其他redis节点。
                    this.redisTemplate.convertAndSend("seckillpush",seckillGoods.getSkuId()+":1");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //  监听哪个用户秒杀的哪个商品！
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SECKILL_USER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER),
            key = {MqConst.ROUTING_SECKILL_USER}
    ))
    public void seckillUser(UserRecode userRecode , Message message , Channel channel){
        try {
            if (userRecode != null){
                seckillGoodsService.seckillOrder(userRecode.getUserId(),userRecode.getSkuId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

}

