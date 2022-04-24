/**
 * @author yjx
 * @date 2021年 12月18日 13:45:41
 */
package com.atguigu.gmall.all;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@ComponentScan({"com.atguigu.gmall"})
@EnableFeignClients(basePackages = {"com.atguigu.gmall"})
@EnableDiscoveryClient
public class WebAllApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebAllApplication.class,args);
    }
}

