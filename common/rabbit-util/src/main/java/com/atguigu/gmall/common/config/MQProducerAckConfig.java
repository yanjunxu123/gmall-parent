/**
 * @author yjx
 * @date 2022年 01月02日 15:55:15
 */
package com.atguigu.gmall.common.config;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class MQProducerAckConfig implements RabbitTemplate.ConfirmCallback,RabbitTemplate.ReturnCallback {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    //  初始化，让rabbitTemplate 与 当前配置类有关系！
    @PostConstruct
    public void init(){
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnCallback(this);
    }
    /**
     * 消息的自动确认
     * @param correlationData
     * @param ack
     * @param cause
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (ack) {
            //消息发送成功
            log.info("消息发送成功： " + JSON.toJSONString(correlationData));
        }else {
            //消息发送失败
            log.info("消息发送失败：" + cause + " 数据：" + JSON.toJSONString(correlationData));
        }
    }

    /**
     * 消息发送失败时
     * @param message
     * @param replyCode
     * @param replyText
     * @param exchange
     * @param routingKey
     */
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        if ("".equals(exchange) && "".equals(routingKey)){
            return;
        }else {
            System.out.println("消息主体: "+new String(message.getBody()));
            System.out.println("应答码： "+replyCode);
            System.out.println("描述：" + replyText);
            System.out.println("消息使用的交换器 exchange : " + exchange);
            System.out.println("消息使用的路由键 routing : "+routingKey);
        }

    }
}

