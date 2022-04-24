/**
 * @author yjx
 * @date 2022年 01月03日 15:05:31
 */
package com.atguigu.gmall.common.service;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
public class RabbitService {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 消息发送
     * @param exchange
     * @param routingKey
     * @param message
     * @return
     */
    public boolean sendMessage(String exchange,String routingKey,Object message){
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        return true;
    }


    public boolean sendDelayMessage(String exchange, String routingKey, Object message, int delayTime){
        this.rabbitTemplate.convertAndSend(exchange, routingKey, message, (msg)->{
                msg.getMessageProperties().setDelay(delayTime*1000);
                return msg;
        });
        return true;
    }
}

