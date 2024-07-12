package com.nageoffer.shortlink.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.admin.common.convention.exception.ClientException;
import com.nageoffer.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.nageoffer.shortlink.admin.dao.entity.UserDo;
import com.nageoffer.shortlink.admin.dao.mapper.UserMapper;
import com.nageoffer.shortlink.admin.dto.resp.UserRespDTO;
import com.nageoffer.shortlink.admin.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * 用户接口实现层
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDo> implements UserService {
    @Override
    public UserRespDTO getUserByUsername(String username) {
        LambdaQueryWrapper<UserDo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserDo::getUsername,username);
        UserDo userDo = baseMapper.selectOne(queryWrapper);
        UserRespDTO userRespDTO = new UserRespDTO();
        if (userDo != null) {
            BeanUtils.copyProperties(userDo, userRespDTO);
            return userRespDTO;
        }else {
            throw new ClientException(UserErrorCodeEnum.USER_NULL);
        }


    }
}
