/**
 * @author yjx
 * @date 2022年 01月14日 19:09:44
 */
package com.atguigu.gmall.activity.controller;

import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.activity.util.CacheHelper;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;

import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.UserRecode;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ConcurrentMap;

@RestController
@RequestMapping("/api/activity/seckill")
public class SeckillGoodsApiController {

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private OrderFeignClient orderFeignClient;
    @GetMapping("findAll")
    public Result findAll(){
        return Result.ok(seckillGoodsService.findAll());
    }
    /**
     * 获取秒杀商品的实体
     * @param skuId
     * @return
     */
    @GetMapping("/getSeckillGoods/{skuId}")
    public Result getSeckillGoods(@PathVariable("skuId") Long skuId){
        return Result.ok(seckillGoodsService.getSeckillGoods(skuId));
    }

    /**
     * 根据用户和商品ID实现秒杀下单
     * @param skuId
     * @param request
     * @return
     */
    @PostMapping("auth/seckillOrder/{skuId}")
    public Result seckillOrder(@PathVariable Long skuId, HttpServletRequest request){
        //校验下单码（抢购码规则可以自定义）
        String userId = AuthContextHolder.getUserId(request);
        String skuIdStr = request.getParameter("skuIdStr");
        if (!skuIdStr.equals(MD5.encrypt(userId))) {
            //请求不合法。
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }
        //校验状态码
        String  state  = (String) CacheHelper.get(skuId.toString());
        if (StringUtils.isEmpty(state)) {
            return  Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }
        if ("1".equals(state)) {
            UserRecode userRecode = new UserRecode();
            userRecode.setUserId(userId);
            userRecode.setSkuId(skuId);
            //mq发送消息，将用户加入秒杀队列中
             rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_SECKILL_USER, MqConst.ROUTING_SECKILL_USER, userRecode);
        }else {
            //已售罄
            return Result.build(null, ResultCodeEnum.SECKILL_FINISH);
        }
        return Result.ok();
    }

    /**
     *
     * @param skuId
     * @param request
     * @return
     */
    @GetMapping(value = "auth/checkOrder/{skuId}")
    public Result checkOrder(@PathVariable("skuId") Long skuId, HttpServletRequest request) {
        //当前登录用户
        String userId = AuthContextHolder.getUserId(request);
        return seckillGoodsService.checkOrder(skuId, userId);
    }

    //  提交订单 ： /auth/submitOrder
    //  接收到前端传递的数据：Json ---> JavaObject
    @PostMapping("/auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request){

        //  获取用户Id，赋值给orderInfo 对象！ 提交订单的时候，前端页面并没有userId
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));
        //  远程调用订单微服务中的提交订单方法！ 将数据保存到orderInfo  +  orderDetai  mysql！
        Long orderId = this.orderFeignClient.submitOrder(orderInfo);
        if(orderId==null){
            return Result.fail().message("提交订单失败!");
        }

        //  如果已经真正下过订单了，则预下单就不需要了！
        //  this.redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).hasKey(userId);
        //  hdel key field
        this.redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).delete(userId);

        //  同时还需要将真正下单数据保存到缓存一份！
        //  Hash key = RedisConst.SECKILL_ORDERS_USERS field = userId  value = orderId
        this.redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).put(userId,orderId.toString());

        //  返回数据！ window.location.href = 'http://payment.gmall.com/pay.html?orderId=' + response.data.data
        return Result.ok(orderId);
    }
}

