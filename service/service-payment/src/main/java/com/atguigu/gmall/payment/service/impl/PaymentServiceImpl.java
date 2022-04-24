/**
 * @author yjx
 * @date 2022年 01月04日 18:45:49
 */
package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService{

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RabbitService rabbitService;
    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("payment_type",paymentType);
        paymentInfoQueryWrapper.eq("order_id",orderInfo.getId());
        Integer integer = paymentInfoMapper.selectCount(paymentInfoQueryWrapper);
        if (integer>0){
            //该订单的支付信息已经存在
            return;
        }
        //没有支付信息，开始创建
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setUserId(orderInfo.getUserId());
        paymentInfo.setPaymentType(paymentType);
        //  paymentInfo.setTradeNo("支付宝的支付交易编号");
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject(orderInfo.getTradeBody());;
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());

        //        异步回调赋值！
        //        paymentInfo.setCallbackTime();
        //        paymentInfo.setCallbackContent();

        //  保存数据
        paymentInfoMapper.insert(paymentInfo);
    }

    //得到交易信息
    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, String paymentType) {
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo);
        paymentInfoQueryWrapper.eq("payment_type",paymentType);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
        //判空 在返回
        if (paymentInfo != null) {
            return paymentInfo;
        }
        return null;
    }

    //修改交易状态
    @Override
    public void paySuccess(String outTradeNo, String paymentType, Map<String, String> paramMap) {


        //获取交易信息
        PaymentInfo paymentInfoQuery = getPaymentInfo(outTradeNo, paymentType);
        if (paymentInfoQuery == null) {
            return;
        }
        //更新状态
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(paramMap.toString());
        paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
        paymentInfo.setTradeNo(paramMap.get("trade_no"));
        updatePaymentInfo(outTradeNo, paymentType, paymentInfo);

        //发送消息，通知订单系统修改订单的状态 通过orderId
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,paymentInfoQuery.getOrderId());
    }

    /**
     * 更新PaymentInfo
     * @param  outTradeNo
     * @param paymentType
     * @param paymentInfo
     */
    public void updatePaymentInfo(String outTradeNo, String paymentType, PaymentInfo paymentInfo) {
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo);
        paymentInfoQueryWrapper.eq("payment_type",paymentType);
        paymentInfoMapper.update(paymentInfo,paymentInfoQueryWrapper);
    }

    @Override
    public void closePayment(Long orderId) {
        //有交易就关闭，没有就不用
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("order_id",orderId);
        Integer integer = paymentInfoMapper.selectCount(paymentInfoQueryWrapper);
        if (integer!=0) {
            //交易记录存在
            //更新交易记录的状态
            PaymentInfo paymentInfo= new PaymentInfo();
            paymentInfo.setPaymentStatus(PaymentStatus.CLOSED.name());
            paymentInfoMapper.update(paymentInfo,paymentInfoQueryWrapper);
        }
    }
}

