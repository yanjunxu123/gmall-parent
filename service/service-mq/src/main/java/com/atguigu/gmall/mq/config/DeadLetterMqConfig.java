/**
 * 基于死信队列实现延迟队列
 *
 * @author yjx
 * @date 2022年 01月03日 16:37:40
 */
package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class DeadLetterMqConfig {
    //定义死信队列所需的常量
    public static final String exchange_dead = "exchange.dead";
    public static final String routing_dead_1 = "routing.dead.1";
    public static final String routing_dead_2 = "routing.dead.2";
    public static final String queue_dead_1 = "queue.dead.1";
    public static final String queue_dead_2 = "queue.dead.2";

    //定义交换机
    @Bean
    public DirectExchange exchange(){
        return new DirectExchange(exchange_dead,true,false,null);
    }
    //正常队列
    @Bean
    public Queue queue1(){
        //正常的队列出现问题，转向死信队列
        HashMap<String, Object> map = new HashMap<>();
        // 参数绑定 此处的key 固定值，不能随意写
        map.put("x-dead-letter-exchange",exchange_dead);
        map.put("x-dead-letter-routing-key",routing_dead_2);
        // 设置延迟时间
        map.put("x-message-ttl",10*1000);
        // 队列名称，是否持久化，是否独享、排外的【true:只可以在本次连接中访问】，是否自动删除，队列的其他属性参数
        return new Queue(queue_dead_1,true,false,false,map);
    }

    // 将队列一 通过routing_dead_1 key 绑定到exchange_dead 交换机上
    @Bean
    public Binding binding(){
        return BindingBuilder.bind(queue1()).to(exchange()).with(routing_dead_1);
    }
    // 这个队列二就是一个普通队列
    @Bean
    public Queue queue2(){
        return new Queue(queue_dead_2,true,false,false,null);
    }
    // 设置队列二的绑定规则
    // 将队列二通过routing_dead_2 key 绑定到exchange_dead交换机上！
    @Bean
    public Binding binding2(){
        return BindingBuilder.bind(queue2()).to(exchange()).with(routing_dead_2);
    }
}

