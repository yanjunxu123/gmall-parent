/**
 * 登录认证过滤
 *
 * @author yjx
 * @date 2021年 12月27日 12:35:48
 */
package com.atguigu.gmall.gateway.filter;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthGlobalFilter implements GlobalFilter {

    @Autowired
    private RedisTemplate redisTemplate;

    //路径匹配工具类
    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    //引入配置文件中的authUrls
    @Value("${authUrls.url}") //使用该注解，当前类必须加入spring容器，且变量名字与配置文件中名称一致。
    private String authUrls;
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //1.过滤内部接口，不让访问内部接口
        //获取用户的请求
        ServerHttpRequest request = exchange.getRequest();
        //获取url
        String path = request.getURI().getPath();
        //根据匹配规则，判断路径
        if (antPathMatcher.match("/**/inner/**",path)) {
            //内部接口，不让直接访问。
            //做出响应
            ServerHttpResponse response = exchange.getResponse();
            return outMsg(response, ResultCodeEnum.PERMISSION);
        }
        //2.访问authUrls中url时，检验用户登录。
        //先得到userId。
        String userId = getUserId(request);
        String userTempId = getUserTempId(request);
        //3.登录之后，获取用户id
        //token有问题
        if("-1".equals(userId)){
            ServerHttpResponse response = exchange.getResponse();
            return outMsg(response, ResultCodeEnum.PERMISSION);
        }
        //用户访问带有*/auth/** 接口时，必须要登录！
        if (antPathMatcher.match("/api/**/auth/**", path)) {
            if (StringUtils.isEmpty(userId)){
                ServerHttpResponse response = exchange.getResponse();
                return outMsg(response, ResultCodeEnum.LOGIN_AUTH);
            }
        }
        String[] split = authUrls.split(",");
        //对url进行验证
        if (split != null && split.length > 0){
            for (String authUrl  : split) {
                //当前的url包含登录的控制器域名，但没有用户id
                if(path.indexOf(authUrl)!=-1 && StringUtils.isEmpty(userId)){
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION,"http://www.gmall.com/login.html?originUrl="+request.getURI());
                    //重定向到登录
                    return response.setComplete();
                }
            }
        }
        //有userId并传递到后端
        if(!StringUtils.isEmpty(userId)){
            request.mutate().header("userId",userId).build();
            // 将现在的request 变成 exchange对象
            return chain.filter(exchange.mutate().request(request).build());
        }

        return chain.filter(exchange);
    }

    private String getUserTempId(ServerHttpRequest request) {
        String userTempId = "";
        List<String> tokenList  = request.getHeaders().get("userTempId");
        if (tokenList != null ) {
             userTempId = tokenList.get(0);
        }else {
            //从cookie中取
            MultiValueMap<String, HttpCookie> cookies = request.getCookies();
            HttpCookie cookie  = cookies.getFirst("userTempId");
            if (cookie != null) {
                userTempId = cookie.getValue();
            }
        }
        return userTempId;
    }

    private String getUserId(ServerHttpRequest request) {
        //拼获取token的key
        String token = "";
        //从请求头中获取token
        List<String> tokenList = request.getHeaders().get("token");
        if (tokenList != null) {
            token = tokenList.get(0);
        }else {
            //请求头中没有token
//            MultiValueMap<String, HttpCookie> cookies = request.getCookies();
//            HttpCookie cookie = cookies.getFirst("token");
            HttpCookie httpCookie = request.getCookies().getFirst("token");
            if (httpCookie != null) {
                token =  httpCookie.getValue();
            }
        }
        if(!StringUtils.isEmpty(token)){
            String loginKey = "user:login:"+token;
            String userStr = (String) redisTemplate.opsForValue().get(loginKey);
            JSONObject userJson  = JSONObject.parseObject(userStr);
            String ip = (String) userJson.get("ip");
            String curIp  = IpUtil.getGatwayIpAddress(request);
            //用IP检验token是否被盗用
            if(ip.equals(curIp)){
                String userId =(String)userJson.getString("userId");
                return userId;
            }else {
                //ip不一致
                return "-1";
            }
        }
        return "";
    }

    private Mono<Void> outMsg(ServerHttpResponse response, ResultCodeEnum resultCodeEnum) {
        //返回用户无权限消息
        Result<Object> result = Result.build(null, resultCodeEnum);
        byte[] bytes = JSON.toJSONString(result).getBytes();

        DataBuffer wrap = response.bufferFactory().wrap(bytes);
        //将数据写入消息头中
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        return response.writeWith(Mono.just(wrap));
    }
}

