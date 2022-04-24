/**
 * @author yjx
 * @date 2021年 12月21日 18:10:45
 */
package com.atguigu.gmall.common.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})//该注解所能使用的位置
@Retention(RetentionPolicy.RUNTIME)  //什么时候起作用
public @interface GmallCache {
    /**
     * 缓存key的前缀
     * @return
     */
    String prefix() default "cache";
}

