/**
 * 商品详情监听商品上架或下架
 *
 * @author yjx
 * @date 2022年 01月03日 15:42:22
 */
package com.atguigu.gmall.list.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.list.service.SearchService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ListReceiver {
    @Autowired
    private SearchService searchService;

    /**
     * 商品上架
     * @param skuId
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_UPPER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_UPPER}
        )
    )
    public void upperGoodsToEs(Long skuId, Message message, Channel channel){
        try {
            if (skuId!=null) {
                //调用服务层方法上架商品
                searchService.upperGoods(skuId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //  确认消息处理！ 第三个参数表示是否重回队列！ 默认会重回3次！ 如何避免重复消费！
        //  借助redis！  setnx : key 不存在的时候生效！ 保证消息幂等性！
        //  channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);
        //  数据记录：   记录 skuId 上架失败。 insert into tabName ();  log 日志.

        //不论消息怎么样，都要有消息的确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    /**
     * 商品下架
     * @param skuId
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_LOWER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_LOWER}
    )
    )
    public void lowerGoodsToEs(Long skuId, Message message, Channel channel){
        try {
            if (skuId!=null) {
                //调用服务层方法上架商品
                searchService.lowerGoods(skuId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //  确认消息处理！ 第三个参数表示是否重回队列！ 默认会重回3次！ 如何避免重复消费！
        //  借助redis！  setnx : key 不存在的时候生效！ 保证消息幂等性！
        //  channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);
        //  数据记录：   记录 skuId 上架失败。 insert into tabName ();  log 日志.

        //不论消息怎么样，都要有消息的确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}

