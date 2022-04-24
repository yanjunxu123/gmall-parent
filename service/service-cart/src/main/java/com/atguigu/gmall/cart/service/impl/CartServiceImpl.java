/**
 * @author yjx
 * @date 2021年 12月28日 18:27:51
 */
package com.atguigu.gmall.cart.service.impl;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ProductFeignClient productFeignClient;

    /**
     * 添加购物车
     * @param skuId
     * @param userId
     * @param skuNum
     */
    @Override
    public void addCart(Long skuId, String userId, Integer skuNum) {

        //向缓存中存储数据，先建立key
        String cartKey = getCartKey(userId);
        //从缓存中获取购物车数据。hget key filed
        BoundHashOperations boundHashOps = this.redisTemplate.boundHashOps(cartKey);
        CartInfo cartInfo = null;
        //通过skuId 判断购物项是否存在 //添加购物车时判断该购物项是否已经存在。
        if (boundHashOps.hasKey(skuId.toString())) {
            //存在
           cartInfo = (CartInfo) boundHashOps.get(skuId.toString());
           cartInfo.setSkuNum(cartInfo.getSkuNum()+skuNum);
           //默认选中
           cartInfo.setCartPrice(productFeignClient.getSkuPrice(skuId));
           //修改更新时间
           cartInfo.setUpdateTime(new Date());
        }else {
            //该购物项不存在
            cartInfo = new CartInfo();
            //赋值给cartInfo
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            cartInfo.setSkuId(skuId);
            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);
            cartInfo.setSkuName(skuInfo.getSkuName());
            //加入购物车时的价格
            cartInfo.setCartPrice(productFeignClient.getSkuPrice(skuId));
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            //实时价格
            cartInfo.setSkuPrice(productFeignClient.getSkuPrice(skuId));
            cartInfo.setCreateTime(new Date());
            cartInfo.setUpdateTime(new Date());
        }
        boundHashOps.put(skuId.toString(),cartInfo);

    }

    /**
     * 查询购物车列表
     * @param userId
     * @param userTempId
     * @return
     */
    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        List<CartInfo> noLoginList = new ArrayList<>();
        //未登录的购物车
        if (!StringUtils.isEmpty(userTempId)) {
            //未登录购物车的key
            String cartKey = getCartKey(userTempId);
            noLoginList = this.redisTemplate.opsForHash().values(cartKey);
        }
        //  排序： 按照修改的时间进行排序！
        if (StringUtils.isEmpty(userId)) {//未登录
            if (!CollectionUtils.isEmpty(noLoginList)){
                noLoginList.stream().sorted((o1, o2) -> {//对未登录的购物车进行
                    return o2.getUpdateTime().compareTo(o1.getUpdateTime());
                });
            }
        }
        //登录的购物车
        //登录购物车的key
        String key = getCartKey(userId);
        BoundHashOperations boundHashOperations = this.redisTemplate.boundHashOps(key); //获取缓存中 购物车的数据
        if (!CollectionUtils.isEmpty(noLoginList)) {
            //既有userId,又有userTempId。循环未登录的购物车，将其与登录的购物车合并。
            noLoginList.stream().forEach(cartInfo -> {
                if (boundHashOperations.hasKey(cartInfo.getSkuId().toString())){//说明具有相同的skuId
                    //取出登录的购物车中的购物项
                    CartInfo cartInfoLogin = (CartInfo) boundHashOperations.get(cartInfo.getSkuId().toString());
                    cartInfoLogin.setSkuNum(cartInfoLogin.getSkuNum()+cartInfo.getSkuNum());//skuId一致，商品数量合并
                    cartInfoLogin.setUpdateTime(new Date()); //修改更新时间，用于排序
                    cartInfoLogin.setSkuPrice(this.productFeignClient.getSkuPrice(cartInfo.getSkuId()));//再次查询实时价格
                    if (cartInfo.getIsChecked()==1) {
                        cartInfoLogin.setIsChecked(1);
                    }
                    //将合并的购物车信息写入缓存 hset key filed value
                    boundHashOperations.put(cartInfo.getSkuId().toString(),cartInfoLogin);
                }else {
                    //没有相同的
                    cartInfo.setUserId(userId);
                    cartInfo.setCreateTime(new Date());
                    cartInfo.setUpdateTime(new Date());
                    //写入缓存
                    boundHashOperations.put(cartInfo.getSkuId().toString(), cartInfo);
                }
            });
            //合并之后删除未登录的购物车
            this.redisTemplate.delete(this.getCartKey(userTempId));
        }
        //查询合并后的购物车数据
        List<CartInfo> loginCartInfoList = boundHashOperations.values();
        //判空操作
        if (CollectionUtils.isEmpty(loginCartInfoList)) {
            return new ArrayList<>(); //为空返回空的集合
        }
        //返回前进行排序
        loginCartInfoList.stream().sorted((o1, o2) -> {
           return o2.getUpdateTime().compareTo(o1.getUpdateTime());
        });
        return loginCartInfoList;
    }

    /**
     * 根据skuId进行，选中状态的修改
     * @param skuId
     * @param userId
     * @param isChecked
     */
    @Override
    public void checkCart(Long skuId, String userId,Integer isChecked) {
        //缓存取出购物车数据，遍历
        String cartKey = this.getCartKey(userId);
        CartInfo cartInfo = (CartInfo) this.redisTemplate.opsForHash().get(cartKey, skuId.toString());
        //判空
        if(cartInfo != null){
            cartInfo.setIsChecked(isChecked);
        }
        //改好之后，放回去
        this.redisTemplate.opsForHash().put(cartKey,skuId.toString(),cartInfo);
    }

    /**
     * 删除购物项
     * @param userId
     * @param skuId
     */
    @Override
    public void deleteCart(String userId ,Long skuId) {
        //  获取购物车key
        String cartKey = this.getCartKey(userId);
        //  删除数据！
        this.redisTemplate.opsForHash().delete(cartKey, skuId.toString());
    }

    /**
     * 获取选中状态的购物车列表
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        String cartKey = this.getCartKey(userId);
        List<CartInfo> cartInfoList = this.redisTemplate.opsForHash().values(cartKey);
        List<CartInfo> cartCheckedList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(cartInfoList)){
             cartCheckedList = cartInfoList.stream().filter(cartInfo -> {
                 //在向订单返回选中状态的数据时，再次查询价格
                 cartInfo.setSkuPrice(this.productFeignClient.getSkuPrice(cartInfo.getSkuId()));
                 return cartInfo.getIsChecked().intValue() == 1;
             }).collect(Collectors.toList());
        }
        return cartCheckedList;
    }

    /**
     * 通过userId,创建购物车的key
     * @param userId
     * @return
     */
    private String getCartKey(String userId) {
        String key = RedisConst.USER_KEY_PREFIX+userId+RedisConst.USER_CART_KEY_SUFFIX;
        return key;
    }


}

