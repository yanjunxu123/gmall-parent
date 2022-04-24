package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface OrderService extends IService<OrderInfo> {
    /**
     * 保存订单信息
     * @param orderInfo
     * @return
     */
    Long saveOrderInfo(OrderInfo orderInfo);

    /**
     * 生成流水线号
     * @param userId
     * @return
     */
    String getTradeNo(String userId);

    /**
     * 比较流水号
     * @param tradeNo
     * @param userId
     * @return
     */
    Boolean checkTradeNo(String tradeNo,String userId);

    /**
     * 删除
     * @param userId
     */
    void delTradeNo(String userId);

    /**
     * 验证库存
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(Long skuId, Integer skuNum);

    /**
     * 获取订单列表
     * @param pageParam
     * @param userId
     * @return
     */
    IPage<OrderInfo> getOrderPageList(Page pageParam, String userId);

    /**
     * 取消订单方法
     * @param orderId
     */
    void cancelOrder(Long orderId);

    /**
     * 根据订单id 获取订单信息
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(Long orderId);

    /**
     * 修改订单的状态
     * @param orderId
     * @param processStatus
     */
    void updateOrderStatus(Long orderId, ProcessStatus processStatus);

    /**
     * 给库存系统发送消息
     * @param orderId
     */
    void sendOrderStatus(Long orderId);

    /**
     * 封装部分数据
     * @param orderInfo
     * @return
     */
    Map<String, Object> initWare(OrderInfo orderInfo);

    /**
     * 拆单
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> orderSplit(Long orderId, String wareSkuMap);

    /**
     * 根据标记取消订单方法
     * @param orderId
     * @param flag
     */
    void cancelOrder(Long orderId, String flag);
}
