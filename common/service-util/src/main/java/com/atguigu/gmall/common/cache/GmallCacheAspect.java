/**
 * AOP实现分布式锁
 * @author yjx
 * @date 2021年 12月21日 18:13:29
 */
package com.atguigu.gmall.common.cache;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.RedisConst;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import springfox.documentation.spring.web.json.Json;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;


@Component
@Aspect
public class GmallCacheAspect {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @SneakyThrows  //lombok中的，自动捕获异常
    @Around("@annotation(com.atguigu.gmall.common.cache.GmallCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint joinPoint){
        Object object = new Object();
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();//获取连接点的信息
        GmallCache gmallCache  = signature.getMethod().getAnnotation(GmallCache.class);//获取方法上的注解
        String prefix = gmallCache.prefix();//获取注解中的前缀
        Object[] args = joinPoint.getArgs();//获取方法传入的参数
        //组装存入缓存的key
        String key = prefix+ Arrays.asList(args).toString();

        try {
            // 从缓存中获取数据
//            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skukey);
            object = cacheHit(key,signature);
            //判断缓存中是否有数据
            if (object == null) {
                //为防止数据库被击穿，应该加锁
                //建一个锁的key‘
                String lockKey = prefix + ":lock";
                //RedisSon创建锁
                //RedisSon锁的类型：
                //             第一种： lock.lock();
                //            第二种:  lock.lock(10,TimeUnit.SECONDS);
                //            第三种： lock.tryLock(100,10,TimeUnit.SECONDS);

                RLock lock = redissonClient.getLock(lockKey);
                boolean res = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                if (res) {
                    try {
                        //得到锁，进行业务
                        //joinPoint.proceed（）执行增强的方法，
                        //joinPoint.getArgs()  获取方法参数
                        object = joinPoint.proceed(joinPoint.getArgs());//通过执行方法，
                        if (object == null) {
                            //为防止缓存穿透，当数据库中无数据时 给缓存一个空的数据
                            //创建一个新对象用于存空数据到缓存中
                            Object skuInfo1 = new Object();
                            redisTemplate.opsForValue().set(key,JSON.toJSONString(skuInfo1), RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                            return skuInfo1;
                        }
                        //查询数据库，有值，同时将数据放入缓存
                        redisTemplate.opsForValue().set(key,JSON.toJSONString(object), RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                        return object;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        lock.unlock();
                    }
                }else {//没有得到锁，进行重试
                    Thread.sleep(1000);
                        return cacheAroundAdvice(joinPoint);
                }
            }else {
                //缓存中有数据
                return object;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //  如果出现问题数据库兜底
        return joinPoint.proceed(joinPoint.getArgs());
    }

    private Object cacheHit(String key, MethodSignature signature) {
        //获取缓存数据
        String  strJson  = (String) redisTemplate.opsForValue().get(key);
        if (!StringUtils.isEmpty(strJson)) {
            //先获取返回的数据的数据类型。
            // 字符串存入缓存之前什么数据类型，就时方法的返回值类型
            Class returnType = signature.getReturnType();
            //  将字串变为当前的返回值类型
            return JSON.parseObject(strJson, returnType);
        }
        return null;
    }
}

