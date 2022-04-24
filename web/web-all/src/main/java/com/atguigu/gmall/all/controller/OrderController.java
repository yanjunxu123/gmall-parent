/**
 * @author yjx
 * @date 2021年 12月29日 19:56:55
 */
package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class OrderController {
    @Autowired
    private OrderFeignClient orderFeignClient;

    @GetMapping("trade.html")
    public String trade(Model model){
        Result<Map<String, Object>> result  = orderFeignClient.trade();
        //获取流水号
        model.addAllAttributes(result.getData());
        return "order/trade";
    }

    /**
     * 我的订单
     * @return
     */
    @GetMapping("myOrder.html")
    public String myOrder(){
        return "order/myOrder";
    }
}

