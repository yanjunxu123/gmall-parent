/**
 * @author yjx
 * @date 2022年 01月04日 18:30:40
 */
package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Calendar;

@Service
public class AlipayServiceImpl implements AlipayService {

    @Autowired
    private AlipayClient alipayClient;
    @Autowired
    private OrderFeignClient orderFeignClient;
    @Autowired
    private PaymentService paymentService;
    @Override
    public String createAlipay(Long orderId) throws AlipayApiException {
        //获取订单信息
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        //在生成二维码之前判断，是否已经有该订单的支付信息
        if("CLOSED".equals(orderInfo.getOrderStatus()) || "PAID".equals(orderInfo.getProcessStatus())){
            //订单已经关闭，或支付
            return "订单已关闭或订单已支付，请重新下单！";
        }
        //生成二维码的时候保存支付信息，用于异步的时候对账
        paymentService.savePaymentInfo(orderInfo, PaymentType.ALIPAY.name());
        //AlipayClient alipayClient =  new DefaultAlipayClient( "https://openapi.alipay.com/gateway.do" , APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);  //获得初始化的AlipayClient
        AlipayTradePagePayRequest alipayRequest =  new  AlipayTradePagePayRequest(); //创建API对应的request
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url); //在公共参数中设置回跳和通知地址
        //用JSON对象填充参数
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("out_trade_no",orderInfo.getOutTradeNo());//out_trade_no	String	必选	商户订单号。由商家自定义，64个字符以内，仅支持字母、数字、下划线且需保证在商户端不重复。
        jsonObject.put("product_code","FAST_INSTANT_TRADE_PAY");//product_code：必填，销售产品码，与支付宝签约的产品码名称。目前电脑支付场景下仅支持 FAST_INSTANT_TRADE_PAY。
        // jsonObject.put("total_amount",orderInfo.getTotalAmount());
        jsonObject.put("total_amount","0.01");//total_amount	Price	必选	11	订单总金额，单位为元，精确到小数点后两位，取值范围为 [0.01,100000000]。金额不能为0。
        jsonObject.put("subject",orderInfo.getTradeBody()); //subject	String	必选	256	订单标题。注意：不可使用特殊字符，如 /，=，& 等。	Iphone6 16G
        //支付二维码的 绝对超时时间：yyyy-MM-dd HH:mm:ss
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE,2);
        jsonObject.put("time_expire",simpleDateFormat.format(calendar.getTime()));
        alipayRequest.setBizContent(jsonObject.toJSONString());
        return alipayClient.pageExecute(alipayRequest).getBody();
    }

    @Override
    public boolean refund(Long orderId) {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        //AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        JSONObject bizContent = new JSONObject();
//        bizContent.put("trade_no", "2021081722001419121412730660");
        bizContent.put("out_trade_no",orderInfo.getOutTradeNo());
        bizContent.put("refund_amount", 0.01);
        bizContent.put("out_request_no", "HZ01RF001");

        //// 返回参数选项，按需传入
        //JSONArray queryOptions = new JSONArray();
        //queryOptions.add("refund_detail_item_list");
        //bizContent.put("query_options", queryOptions);

        request.setBizContent(bizContent.toString());
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
            //更改订单的状态
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setPaymentStatus(PaymentStatus.CLOSED.name());
            paymentService.updatePaymentInfo(orderInfo.getOutTradeNo(),PaymentType.ALIPAY.name(),paymentInfo);

            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }

    }

    @Override
    public Boolean closePayload(Long orderId) {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        //AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("trade_no",orderInfo.getOutTradeNo());
        request.setBizContent(bizContent.toString());
        AlipayTradeCloseResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    @Override
    public Boolean checkPayment(Long orderId) {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        //AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        //bizContent.put("trade_no", "2014112611001004680073956707");
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }
}

