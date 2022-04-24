package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;

import java.util.Map;

public interface PaymentService {
    /**
     * 保存支付信息
     * @param orderInfo
     * @param paymentType
     */
    void savePaymentInfo(OrderInfo orderInfo, String paymentType);

    /**
     * 根据订单业务号，支付方式查询交易信息
     * @param outTradeNo
     * @param paymentType
     * @return
     */
    PaymentInfo getPaymentInfo(String outTradeNo, String paymentType);

    /**
     * 修改交易状态
     * @param outTradeNo
     * @param paymentType
     * @param paramMap
     */
    void paySuccess(String outTradeNo, String paymentType, Map<String, String> paramMap);

    /**
     * 更新订单状态
     * @param outTradeNo
     * @param paymentType
     * @param paymentInfo
     */
    void updatePaymentInfo(String outTradeNo, String paymentType, PaymentInfo paymentInfo);

    /**
     * 关闭本地交易
     * @param orderId
     */
    void closePayment(Long orderId);
}
