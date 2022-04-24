/**
 * @author yjx
 * @date 2021年 12月29日 20:26:25
 */
package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper,OrderInfo>implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${ware.url}")
    private String wareUrl;

    @Autowired
    private RabbitService rabbitService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrderInfo(OrderInfo orderInfo) {
        //  编写插入数据实现！
        /*  需要赋值的字段：
          total_amount
          order_status
          user_id  控制器已经赋值
          out_trade_no
          trade_body
          expire_time
          operate_time
          process_status
          */
        orderInfo.sumTotalAmount(); //total_amount

        orderInfo.setOrderStatus(OrderStatus.UNPAID.name()); //  order_status

        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(1000); //out_trade_no
        orderInfo.setOutTradeNo(outTradeNo);

        orderInfo.setTradeBody("啤酒饮料矿泉水,花生,瓜子,八宝粥。来来来把腿收一收.."); //trade_body

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime()); //expire_time

        orderInfo.setOperateTime(new Date()); //

        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name()); //process_status
        //插入orderInfo信息
        orderInfoMapper.insert(orderInfo);

        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        orderDetailList.forEach(orderDetail -> {
            orderDetail.setOrderId(orderInfo.getId());
            //插入orderDetail信息
            orderDetailMapper.insert(orderDetail);
        });


        //发送延迟队列，如果定时未支付，取消订单
        rabbitService.sendDelayMessage(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL, MqConst.ROUTING_ORDER_CANCEL, orderInfo.getId(), MqConst.DELAY_TIME);

        //获取并返回orderId
        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        String tradeNo = UUID.randomUUID().toString();
        //key
        String tradeNoKey = "tradeNo:"+userId;
        this.redisTemplate.opsForValue().set(tradeNoKey,tradeNo);
        return tradeNo;
    }

    @Override
    public Boolean checkTradeNo(String tradeNo, String userId) {
        //  生成key
        String tradeNoKey = "tradeNo:"+userId;
        String tradeRedis = (String) this.redisTemplate.opsForValue().get(tradeNoKey);

        return tradeNo.equals(tradeRedis);
    }

    @Override
    public void delTradeNo(String userId) {
        //  生成key
        String tradeNoKey = "tradeNo:"+userId;
        this.redisTemplate.delete(tradeNoKey);
    }

    @Override
    public boolean checkStock(Long skuId, Integer skuNum) {
        // 远程调用http://localhost:9001/hasStock?skuId=10221&num=2
        String result = HttpClientUtil.doGet(wareUrl + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(result);
    }

    /**
     * 获取订单列表
     * @param pageParam
     * @param userId
     * @return
     */
    @Override
    public IPage<OrderInfo> getOrderPageList(Page pageParam, String userId) {
        IPage<OrderInfo> orderInfoIPage = orderInfoMapper.selectPageByUserId(pageParam,userId);
        orderInfoIPage.getRecords().forEach(orderInfo -> {
            orderInfo.setOrderStatusName(OrderStatus.getStatusNameByStatus(orderInfo.getOrderStatus()));
        });
        return orderInfoIPage;
    }


    @Override
    public void cancelOrder(Long orderId) {

        updateOrderStatus(orderId,ProcessStatus.CLOSED);
        //发送消息，关闭交易()
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);
    }

    /**
     * 根据订单id获取订单信息
     * @param orderId
     * @return
     */
    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        QueryWrapper<OrderDetail> orderDetailQueryWrapper = new QueryWrapper<>();
        orderDetailQueryWrapper.eq("order_id",orderId);
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(orderDetailQueryWrapper);
        orderInfo.setOrderDetailList(orderDetails);
        return orderInfo;
    }

    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus.name());
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfoMapper.updateById(orderInfo);
    }

    @Override
    public void sendOrderStatus(Long orderId) {
        //修改订单的状态
        updateOrderStatus(orderId,ProcessStatus.NOTIFIED_WARE);
      //先得到orderInfo
        OrderInfo orderInfo = getOrderInfo(orderId);
        Map<String,Object> map = initWare(orderInfo);

        //发送消息给仓库系统，数据格式根据接口时json
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_WARE_STOCK,MqConst.ROUTING_WARE_STOCK, JSON.toJSONString(map));
    }

    public Map<String, Object> initWare(OrderInfo orderInfo) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId",orderInfo.getId());
        map.put("consignee",orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody",orderInfo.getTradeBody());
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay", "2");
        map.put("wareId", orderInfo.getWareId());// 仓库Id ，减库存拆单时需要使用！
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        List<HashMap<String, Object>> maps = orderDetailList.stream().map(orderDetail -> {
            HashMap<String, Object> detailMap = new HashMap<>();
            detailMap.put("skuId", orderDetail.getSkuId());
            detailMap.put("skuNum", orderDetail.getSkuNum());
            detailMap.put("skuName", orderDetail.getSkuName());
            return detailMap;
        }).collect(Collectors.toList());
        //添加订单明细
        map.put("details",maps);
        //返回数据
        return map;
    }

    @Override
    public List<OrderInfo> orderSplit(Long orderId, String wareSkuMap) {
        //搞个存储子订单的集合
        List<OrderInfo> orderInfoList = new ArrayList<>();
        //获取订单的原始数据
        OrderInfo orderInfo = getOrderInfo(orderId);
        //  2.  wareSkuMap [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]将其转换为能处理的对象！
        List<Map> mapList = JSON.parseArray(wareSkuMap, Map.class);
        if (!CollectionUtils.isEmpty(mapList)){
            for (Map map : mapList) {
                String wareId = (String) map.get("wareId");
               List<String>  skuIds = (List<String>) map.get("skuIds");
                //创建子订单，给子订单赋值
                OrderInfo subOrderInfo = new OrderInfo();
                //  属性拷贝，将部分字段重新赋值
                BeanUtils.copyProperties(orderInfo,subOrderInfo);
                //一个订单会拆出来多个订单，id置空让其自增
                subOrderInfo.setId(null);
                //计算每个子订单的金额
                List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
                //子订单订单明细集合
                List<OrderDetail> subOrderDetailList=new ArrayList<>();
                if(!CollectionUtils.isEmpty(orderDetailList)){
                    //订单明细的skuId与skuIds中的做对应
                    for (OrderDetail orderDetail : orderDetailList) {
                        for (String skuId : skuIds) {
                            if (orderDetail.getSkuId().compareTo(Long.parseLong(skuId))==0) {
                                subOrderDetailList.add(orderDetail);
                            }
                        }
                    }
                }
                //将子订单的订单明细赋值给子订单
                subOrderInfo.setOrderDetailList(subOrderDetailList);
                //计算子订单的金额
                subOrderInfo.sumTotalAmount();
                //给子订单赋值父订单id
                subOrderInfo.setParentOrderId(orderId);
                //设置仓库id
                subOrderInfo.setWareId(wareId);
                //保存子订单到数据库
                saveOrderInfo(subOrderInfo);
                orderInfoList.add(subOrderInfo);
            }

        }
        //拆完，给父订单更改一下状态
        updateOrderStatus(orderId,ProcessStatus.SPLIT);
        //返回
        return orderInfoList;
    }

    @Override
    public void cancelOrder(Long orderId, String flag) {
       //更新订单状态
       updateOrderStatus(orderId,ProcessStatus.CLOSED);
       if ("2".equals(flag)){
           //（暂认为只有支付宝一种支付方式）给支付发送消息，
           rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);
       }
    }
}

