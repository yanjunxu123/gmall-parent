/**
 * @author yjx
 * @date 2022年 01月14日 19:24:44
 */
package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.activity.client.ActivityFeignClient;
import com.atguigu.gmall.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Controller
public class SeckillController {
    @Autowired
    private ActivityFeignClient activityFeignClient;

    @GetMapping("seckill.html")
    public String index(Model model){
        Result all = activityFeignClient.findAll();
        model.addAttribute("list", all.getData());
        return "seckill/index";
    }

    @GetMapping("seckill/{skuId}.html")
    public String getItem(@PathVariable Long skuId, Model model){
        Result result = activityFeignClient.getSeckillGoods(skuId);
        model.addAttribute("item",result.getData());
        return "seckill/item";
    }

    @GetMapping("seckill/queue.html")
    public String queue(@RequestParam(name = "skuId") Long skuId,
                        @RequestParam(name = "skuIdStr") String skuIdStr,
                        HttpServletRequest request){
        request.setAttribute("skuId",skuId);
        request.setAttribute("skuIdStr",skuIdStr);
        return "seckill/queue";
    }
    //  去下单控制器：
    @GetMapping("seckill/trade.html")
    public String seckillTrade(Model model){
        //  后台需要存储 userAddressList detailArrayList totalAmount totalNum
        Result<Map<String,Object>> result =  this.activityFeignClient.trade();
        //  判断
        if (result.isOk()){
            model.addAllAttributes(result.getData());
            return "seckill/trade";
        }else {
            //  ${message}
            model.addAttribute("message","下单失败!");
            return "seckill/fail";
        }


    }
}

