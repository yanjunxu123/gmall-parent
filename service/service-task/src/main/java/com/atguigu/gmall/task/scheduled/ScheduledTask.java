/**
 * @author yjx
 * @date 2022年 01月11日 18:26:41
 */
package com.atguigu.gmall.task.scheduled;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling //开启定时器的注解
public class ScheduledTask  {
    @Autowired
    private RabbitService rabbitService;
    //发送扫描数据库，添加秒杀商品的消息
    @Scheduled(cron = "0/10 * * * * ?")
    public void sendKillGoodsTask(){
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK,MqConst.ROUTING_TASK_1,"扫描秒杀");
    }
}

