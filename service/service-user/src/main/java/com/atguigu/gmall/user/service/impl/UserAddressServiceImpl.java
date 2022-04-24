/**
 * @author yjx
 * @date 2021年 12月29日 18:55:32
 */
package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.mapper.UserAddressMapper;
import com.atguigu.gmall.user.service.UserAddressService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserAddressServiceImpl implements UserAddressService {

    @Autowired
    private UserAddressMapper userAddressMapper;
    @Override
    public List<UserAddress> findUserAddressListByUserId(String userId) {
        QueryWrapper<UserAddress> userAddressQueryWrapper = new QueryWrapper<>();
        userAddressQueryWrapper.eq("user_id",userId);
        List<UserAddress> userAddressList = userAddressMapper.selectList(userAddressQueryWrapper);
        return userAddressList;
    }
}

