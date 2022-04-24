/**
 * @author yjx
 * @date 2022年 01月04日 18:17:48
 */
package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("api/payment/alipay")
public class AlipayController {
    @Autowired
    private AlipayService alipayService;

    @Autowired
    private PaymentService paymentService;

    @Value("${app_id}")
    private String app_id;

    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 生成二维码并支付
     * @param orderId
     * @return
     */
    @GetMapping("submit/{orderId}")
    @ResponseBody
    public String aliPay(@PathVariable Long orderId)  {
        String form = null;
        try {
            form = alipayService.createAlipay(orderId);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return form;
    }

    //  同步回调地址：
    //  http://api.gmall.com/api/payment/alipay/callback/return
    @GetMapping("callback/return")
    public String callbackReturn(){
        //重定向到支付成功的页面
        //   http://payment.gmall.com/pay/success.html
        return "redirect:"+ AlipayConfig.return_order_url;
    }

    //异步回调
    //
    @PostMapping("callback/notify")
    @ResponseBody
    public String callbackReturnNotify(@RequestParam Map<String, String> paramMap){
        //   http://rjsh38.natappfree.cc/api/payment/alipay/callback/notify
        System.out.println("异步回调进来了。。。。。");
        //Map<String, String> paramsMap = ... //将异步通知中收到的所有参数都存放到map中
        boolean signVerified = false;//调用SDK验证签名
        try {
            signVerified = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        //获取异步通知的参数中的订单号
        String outTradeNo = paramMap.get("out_trade_no");
        //获取异步通知的参数中的订单总金额
        String totalAmount = paramMap.get("total_amount");
        //获取异步通知的参数中的应用id
        String appId = paramMap.get("app_id");
        //获取异步通知的参数中的交易状态
        String tradeStatus = paramMap.get("trade_status");
        //  保证异步通知的幂等性！notify_id
        String notifyId = paramMap.get("notify_id");
        PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());

        if(signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            if (paymentInfo == null || new BigDecimal("0.01").compareTo(new BigDecimal(totalAmount))!=0
                || !app_id.equals(appId)) { //验证 电商系统中交易信息、订单总金额、应用id与支付宝异步通知的参数是否相同
                return "failure";
            }
            //当商户收到服务器异步通知并打印出 success 时，服务器异步通知参数 notify_id 才会失效。
            // 也就是说在支付宝发送同一条异步通知时（包含商户并未成功打印出 success 导致支付宝重发数次通知），
            // 服务器异步通知参数 notify_id 是不变的。
            //通过redis验证是否是同一个notify_id，同一个不进行异步的处理，不是同一个将继续进行异步处理，返回success
            Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(notifyId, notifyId, 14462, TimeUnit.MINUTES);
            if (!aBoolean){
                return "failure";
            }
            if ("TRADE_SUCCESS".equals(tradeStatus)||"TRADE_FINISHED".equals(tradeStatus)) {
                //修改交易状态，在修改订单的状态。
                paymentService.paySuccess(outTradeNo,PaymentType.ALIPAY.name(),paramMap);
                //
                return "success";
            }
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }
    /**
     * 退款
     * @param orderId
     * @return
     */
    @GetMapping("refund/{orderId}")
    @ResponseBody
    public Result refund(@PathVariable Long orderId){
       boolean flag = alipayService.refund(orderId);
       return Result.ok(flag);
    }

    /**
     * 根据订单id关闭订单
     * @param orderId
     * @return
     */
    @GetMapping("closePay/{orderId}")
    @ResponseBody
    public Boolean closePayload(@PathVariable Long orderId) {
        Boolean aBoolean  = alipayService.closePayload(orderId);
        return aBoolean;
    }

    // 查看是否有交易记录
    /**
     * 统一收单线下交易查询
     */
    @RequestMapping("checkPayment/{orderId}")
    @ResponseBody
    public Boolean checkPayment(@PathVariable Long orderId) {
        Boolean flag = alipayService.checkPayment(orderId);
        return flag;
    }


    @GetMapping("getPaymentInfo/{outTradeNo}")
    @ResponseBody
    public PaymentInfo  getPaymentInfo(@PathVariable String outTradeNo){
        PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
        if (paymentInfo != null) {
            return paymentInfo;
        }
        return null;
    }
}

