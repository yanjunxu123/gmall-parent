/**
 * @author yjx
 * @date 2021年 12月21日 19:52:40
 */
package com.atguigu.gmall.item.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {
    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        //定义线程池 核心线程数的确定：io密集型 cpu核心数的2倍 cpu密集型： cpu核心数+1
            ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                    9,
                    15,
                    3,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(3)
            );
            return threadPoolExecutor;
    }
}

