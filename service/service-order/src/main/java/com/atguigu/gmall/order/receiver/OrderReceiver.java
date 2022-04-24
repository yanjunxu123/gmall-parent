/**
 * 订单消息监听
 *
 * @author yjx
 * @date 2022年 01月03日 17:18:16
 */
package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
public class OrderReceiver {
    @Autowired
    private OrderService orderService;
    @Autowired
    private PaymentFeignClient paymentFeignClient;
    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void cancelOrder(Long orderId, Message message, Channel channel){
        try {
            if (orderId!=null) {
                OrderInfo orderInfo = orderService.getById(orderId);
                //  判断支付状态,进度状态
                if (orderInfo != null && "UNPAID".equals(orderInfo.getOrderStatus()) && "UNPAID".equals(orderInfo.getProcessStatus())) {
//                    //  关闭订单
//                    orderService.execExpiredOrder(orderId);

                    PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                    //判断是否有本地交易
                    if (paymentInfo != null && "UNPAID".equals(paymentInfo.getPaymentStatus())) {
                       //支付平台判断是否有交易记录
                        Boolean aBoolean = paymentFeignClient.checkPayment(orderId);
                        if (aBoolean){
                          //判断取消平台交易记录成功与否
                            Boolean flag = paymentFeignClient.closePay(orderId);
                            if (flag) {
                                //成功取消平台交易记录，所有的都取消
                                orderService.cancelOrder(orderId,"2");
                            }else {
                                //取消平台交易失败，说明付款成功了
                            }
                        }else {
                            //平台无交易记录 关闭 本地交易记录和订单
                            orderService.cancelOrder(orderId,"2");
                        }
                    }else {
                        //没有交易记录，关闭订单
                        orderService.cancelOrder(orderId,"1");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //  手动确认消息 如果不确认，有可能会到消息残留。
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
        )
    )
    public void updateOrderStatus(Long orderId, Message message, Channel channel){
        try {
            if (orderId!=null) {
              //修改订单的状态
                orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
                //修改之后，发送消息给库存系统
                orderService.sendOrderStatus(orderId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //  手动确认消息 如果不确认，有可能会到消息残留。
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }


    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    )
    )
    public void wareOrder(String strJson, Message message, Channel channel){
        try {
            if (!StringUtils.isEmpty(strJson)) {
                Map map = JSON.parseObject(strJson, Map.class);
                String orderId = (String) map.get("orderId");
                Object status = map.get("status");
                //判断status
                if ("DEDUCTED".equals(status)){
                    //减库存成功
                    orderService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.WAITING_DELEVER);
                }else {
                    //减库存失败
                    orderService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.STOCK_EXCEPTION);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //  手动确认消息 如果不确认，有可能会到消息残留。
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}

