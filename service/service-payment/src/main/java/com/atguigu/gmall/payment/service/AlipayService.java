package com.atguigu.gmall.payment.service;

import com.alipay.api.AlipayApiException;

public interface AlipayService {
    /**
     * 生成二维码
     * @param orderId
     * @return
     */
    String createAlipay(Long orderId) throws AlipayApiException;

    /**
     * 退款
     * @param orderId
     * @return
     */
    boolean refund(Long orderId);

    /**
     * 关闭交易接口
     * @param orderId
     * @return
     */
    Boolean closePayload(Long orderId);

    /**
     *
     * @param orderId
     * @return
     */
    Boolean checkPayment(Long orderId);
}
