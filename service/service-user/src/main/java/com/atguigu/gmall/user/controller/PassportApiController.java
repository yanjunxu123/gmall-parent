/**
 * @author yjx
 * @date 2021年 12月25日 19:45:05
 */
package com.atguigu.gmall.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.IpUtil;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user/passport")
public class PassportApiController {
    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate redisTemplate;


    @PostMapping("login")
    public Result login(@RequestBody UserInfo userInfo, HttpServletRequest request){
        UserInfo info = userService.login(userInfo);
        if (info != null){

            //有登录信息
            //返回token给前端，并将token放入缓存
            HashMap<String, Object> map  = new HashMap<>();
            String token = UUID.randomUUID().toString();
            map.put("token",token);
            map.put("nickName",info.getNickName());
            //定义缓存的key
            String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
            JSONObject userJson  = new JSONObject();
            userJson.put("userId",info.getId().toString());//用于后续得到用户id使用
            String ipAddress = IpUtil.getIpAddress(request);
            userJson.put("ip", ipAddress);// 防止有人使用盗用的token进行查询用户id，加上IP进行防伪。
            //放缓存
            redisTemplate.opsForValue().set(userKey, userJson.toJSONString(), RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);
            return Result.ok(map);

        }else {
            //登录失败
            return Result.fail().message("用户名或密码错误");
        }
    }
    @GetMapping("logout")
    public Result logout(@RequestHeader String token,HttpServletRequest request){
        String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
        redisTemplate.delete(userKey);
        return Result.ok();
    }

}

