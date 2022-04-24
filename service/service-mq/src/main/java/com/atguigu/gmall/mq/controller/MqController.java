/**
 * 发送消息
 * @author yjx
 * @date 2022年 01月03日 15:08:15
 */
package com.atguigu.gmall.mq.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.atguigu.gmall.mq.config.DelayedMqConfig;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/mq")
public class MqController {
    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @GetMapping("sendConfirm")
    public Result sendConfirm(){
        rabbitService.sendMessage("exchange.confirm","routing.confirm","来人了，开始接客吧！");
        return Result.ok();
    }

    /**
     * 基于死信队列的延时队列的消息发送
     * @return
     */
    @GetMapping("sendDeadLettle")
    public Result sendDeadLettle(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.rabbitTemplate.convertAndSend(DeadLetterMqConfig.exchange_dead,DeadLetterMqConfig.routing_dead_1,"iuok");
        System.out.println(simpleDateFormat.format(new Date()) + " Delay sent.");
        return Result.ok();
    }

    @GetMapping("sendDelay")
    public Result sendDelay(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.rabbitTemplate.convertAndSend(DelayedMqConfig.exchange_delay, DelayedMqConfig.routing_delay, sdf.format(new Date()), new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                message.getMessageProperties().setDelay(10*1000);
                System.out.println(sdf.format(new Date()) + " Delay sent.");
                return message;
            }
        });
        return Result.ok();
    }


}

