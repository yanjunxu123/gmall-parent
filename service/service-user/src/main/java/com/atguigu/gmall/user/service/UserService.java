package com.atguigu.gmall.user.service;


import com.atguigu.gmall.model.user.UserInfo;

public interface UserService {
    /**
     * 登录方法
     * @return
     */
    UserInfo login(UserInfo userInfo);
}
