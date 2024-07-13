package com.nageoffer.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.admin.common.convention.exception.ClientException;
import com.nageoffer.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.nageoffer.shortlink.admin.dao.entity.UserDo;
import com.nageoffer.shortlink.admin.dao.mapper.UserMapper;
import com.nageoffer.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserRespDTO;
import com.nageoffer.shortlink.admin.service.UserService;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.nageoffer.shortlink.admin.common.constant.RedisCacheConstant.LOCK_USER_REGISTER_KEY;
import static com.nageoffer.shortlink.admin.common.enums.UserErrorCodeEnum.USER_NAME_EXIST;

/**
 * 用户接口实现层
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDo> implements UserService {
    @Autowired
    private RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    @Autowired
    private RedissonClient redissonClient;

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

    @Override
    public Boolean hasUsername(String username) {
//        LambdaQueryWrapper<UserDo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
//        lambdaQueryWrapper.eq(UserDo::getUsername,username);
//        UserDo userDo = this.getOne(lambdaQueryWrapper);
//        return null == userDo;
        return !userRegisterCachePenetrationBloomFilter.contains(username);
    }

    @Override
    public void register(UserRegisterReqDTO requestParam) {
        if (!hasUsername(requestParam.getUsername())) {
            throw new ClientException(USER_NAME_EXIST);
        }
        RLock lock = redissonClient.getLock(LOCK_USER_REGISTER_KEY + requestParam.getUsername());
        try {
            if (lock.tryLock()) {


                int insert = baseMapper.insert(BeanUtil.toBean(requestParam, UserDo.class));
                if (insert < 1) {
                    throw new ClientException(UserErrorCodeEnum.USER_SAVE_ERROR);
                }
                userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());
            }
            throw new ClientException(USER_NAME_EXIST);
        } finally {
            lock.unlock();
        }


    }
}
