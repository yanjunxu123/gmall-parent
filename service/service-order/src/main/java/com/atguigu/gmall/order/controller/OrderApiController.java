/**
 * @author yjx
 * @date 2021年 12月29日 19:32:06
 */
package com.atguigu.gmall.order.controller;


import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/order")
public class OrderApiController {
    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    /**
     * 确认订单
     *
     * @param request
     * @return
     */
    @GetMapping("auth/trade")
    public Result<Map<String, Object>> trade(HttpServletRequest request) {
        HashMap<String, Object> map = new HashMap<>();
        String userId = AuthContextHolder.getUserId(request);
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);//获取送货地址
        List<CartInfo> cartInfoList = cartFeignClient.getCartCheckedList(userId); //获取用户要购买的商品

        //声明集合存储订单明细
        List<OrderDetail> orderDetails = new ArrayList<>();
        AtomicInteger skuNum = new AtomicInteger(); //明细的商品数量
        if (!CollectionUtils.isEmpty(cartInfoList)) {
            orderDetails = cartInfoList.stream().map(cartInfo -> {
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setOrderPrice(cartInfo.getSkuPrice());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setImgUrl(cartInfo.getImgUrl());

                skuNum.set(skuNum.get() + cartInfo.getSkuNum());
                return orderDetail;
            }).collect(Collectors.toList());
        }
        OrderInfo orderInfo = new OrderInfo();//
        orderInfo.setOrderDetailList(orderDetails);
        orderInfo.sumTotalAmount();

        //map 保存数据返回
        map.put("userAddressList", userAddressList);
        map.put("detailArrayList", orderDetails);
        map.put("totalAmount", orderInfo.getTotalAmount());
        map.put("totalNum", skuNum);//两种： 一种：计算集合的长度 ，第二种计算集合中每个商品的skuNum !
        map.put("tradeNo", orderService.getTradeNo(userId));
        return Result.ok(map);
    }

    /**
     * 提交订单
     * @param orderInfo
     * @param request
     * @return
     */
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));

        //比较流水号
        String tradeNo = request.getParameter("tradeNo");
        Boolean result = this.orderService.checkTradeNo(tradeNo, userId);
        if (!result) {
            //  返回信息提示！
            return Result.fail().message("不能回退无刷新提交订单!");
        }
        //删除缓存中的流水号
        orderService.delTradeNo(userId);

        //声明一个用于存储错误信息的集合
        List<String> errorList = new ArrayList<>();
        //声明一个用于储存线程的集合
        List<CompletableFuture> completableFutures = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (!CollectionUtils.isEmpty(orderDetailList)) {
            for (OrderDetail orderDetail : orderDetailList) {
                //验证库存
                CompletableFuture<Void> checkStockCompletableFuture = CompletableFuture.runAsync(() -> {
                    boolean b = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
                    if (!b) {
                        errorList.add(orderDetail.getSkuName() + "库存不足!");
                    }
                },threadPoolExecutor);
                completableFutures.add(checkStockCompletableFuture);

                //验证价格
                CompletableFuture<Void> checkPriceCompletableFuture = CompletableFuture.runAsync(() -> {

                    BigDecimal skuPrice = this.productFeignClient.getSkuPrice(orderDetail.getSkuId());
                    //价格变动
                    if (skuPrice.compareTo(orderDetail.getOrderPrice()) != 0) {
                        //取已选择的购物车数据
                        List<CartInfo> cartCheckedList = this.cartFeignClient.getCartCheckedList(userId);
                        cartCheckedList.forEach(cartInfo -> {
                            //将有价格变动的购物车数据再次放入缓存，以做更新
                            String cartKey = RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
                            this.redisTemplate.opsForHash().put(cartKey, orderDetail.getSkuId().toString(), cartInfo);
                        });
                        //提示价格变动
                        BigDecimal orderPrice = orderDetail.getOrderPrice();
                        BigDecimal subtract = orderPrice.subtract(skuPrice);
                        String msg = "";
                        if (subtract.intValue() > 0) {
                            msg = "比原来降价:" + subtract.abs();
                        } else {
                            msg = "比原来上涨:" + subtract.abs();
                        }
                        //  return Result.fail().message(orderDetail.getSkuName() + ":\t价格有变动,重新生成订单!" + msg);
                        errorList.add(orderDetail.getSkuName() + ":\t价格有变动,重新生成订单!" + msg);
                    }
                },threadPoolExecutor);
                completableFutures.add(checkPriceCompletableFuture);
            }
        }

        //任务组合
        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[completableFutures.size()])).join();
        //  是否有异常呢？
        if(errorList.size()>0){
            return Result.fail().message(StringUtils.join(errorList,","));
        }
        Long orderId = orderService.saveOrderInfo(orderInfo);// 验证通过，保存订单！返回一个订单号
        return Result.ok(orderId);
    }

    /**
     * 用户订单列表
     * @param page
     * @param limit
     * @param request
     * @return
     */
    @GetMapping("/auth/{page}/{limit}")
    public Result getOrderPageList(
            @PathVariable Long page,
            @PathVariable Long limit,
            HttpServletRequest request
            ){
        String userId = AuthContextHolder.getUserId(request);   //获取用户id， 需要知道是那个用户的订单列表
        Page pageParam = new Page(page, limit);
        IPage<OrderInfo> iPage = orderService.getOrderPageList(pageParam,userId);
        return Result.ok(iPage);
    }

    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId) {
       return orderService.getOrderInfo(orderId);
    }


    @PostMapping("orderSplit")
    public List<Map> orderSplit(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");
        List<Map> maps = new ArrayList<>();
        List<OrderInfo> subOrderInfoList = orderService.orderSplit(Long.parseLong(orderId),wareSkuMap);
        if (!CollectionUtils.isEmpty(subOrderInfoList)) {
            for (OrderInfo orderInfo : subOrderInfoList) {
                Map<String,Object> map = orderService.initWare(orderInfo);
                maps.add(map);
            }
        }

        return maps;
    }

    //  提供一个秒杀订单的数据接口：
    @PostMapping("inner/seckill/submitOrder")
    public Long seckillSubmitOrder(@RequestBody OrderInfo orderInfo){
        //  调用订单服务层的方法！
        Long orderId = this.orderService.saveOrderInfo(orderInfo);
        //  返回订单Id
        return orderId;
    }
}


