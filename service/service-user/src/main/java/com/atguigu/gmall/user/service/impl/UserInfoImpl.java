/**
 * @author yjx
 * @date 2021年 12月25日 19:43:18
 */
package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.tomcat.util.digester.Digester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
@Service
public class UserInfoImpl implements UserService {
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Override
    public UserInfo login(UserInfo userInfo) {
        //查询是否登录成功
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper.eq("login_name",userInfo.getLoginName());
        String newPwd = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes());
        userInfoQueryWrapper.eq("passwd",newPwd);
        UserInfo info = userInfoMapper.selectOne(userInfoQueryWrapper);
        //判断用户是否存在
        if (info != null) {
            return info;
        }
        return null;
    }
}

