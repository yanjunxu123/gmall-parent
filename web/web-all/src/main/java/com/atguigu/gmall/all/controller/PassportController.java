/**
 * @author yjx
 * @date 2021年 12月28日 10:05:29
 */
package com.atguigu.gmall.all.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@Controller
public class PassportController {
    @GetMapping("login.html")
    public String login(HttpServletRequest request) {
            //登录成功，根据originUrl来决定跳转回之前的页面
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl",originUrl);
        return "login";
    }
}

