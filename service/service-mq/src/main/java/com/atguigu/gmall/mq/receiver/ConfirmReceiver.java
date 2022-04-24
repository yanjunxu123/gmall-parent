/**
 * 消息接收方（消费者--测试用）
 *
 * @author yjx
 * @date 2022年 01月03日 15:11:31
 */
package com.atguigu.gmall.mq.receiver;

import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;



@Component
public class ConfirmReceiver {
    /**
     * 消费方监听消息
     * 什么时候需要绑定： 当交换机，路由键，队列没有绑定时进行绑定（bindings）
     * 什么时候不需要绑定 ： 当交换机，路由键，队列进行过绑定
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.confirm",autoDelete = "false"),
            exchange = @Exchange(value = "exchange.confirm",autoDelete = "true"),
            key = {"routing.confirm"}
                                            )
    )
    public void process(Message message, Channel channel){
        System.out.println("message.getBody() = " + new String(message.getBody()));
        //false 表示确认一个消息 true 表示批量确认消息
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}

