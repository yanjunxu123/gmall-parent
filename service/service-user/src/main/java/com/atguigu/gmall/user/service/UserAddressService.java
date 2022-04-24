package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserAddress;

import java.util.List;

public interface UserAddressService {
    /**
     * 通过用户id
     * @param userId
     * @return
     */
    List<UserAddress> findUserAddressListByUserId(String userId);
}
