package com.nageoffer.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.admin.common.constant.RedisCacheConstant;
import com.nageoffer.shortlink.admin.common.convention.exception.ClientException;
import com.nageoffer.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.nageoffer.shortlink.admin.dao.entity.UserDo;
import com.nageoffer.shortlink.admin.dao.mapper.UserMapper;
import com.nageoffer.shortlink.admin.dto.req.UserLoginReqDTO;
import com.nageoffer.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.nageoffer.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserRespDTO;
import com.nageoffer.shortlink.admin.service.UserService;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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
        return userRegisterCachePenetrationBloomFilter.contains(username);
    }

    @Override
    public void register(UserRegisterReqDTO requestParam) {
        boolean hasUserName = hasUsername(requestParam.getUsername());
        if (hasUserName) {
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
                return;
                //退出try方法
            }
            throw new ClientException(USER_NAME_EXIST);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void updateByReqDTO(UserUpdateReqDTO requestParam) {
        // TODO 验证当前用户名是否为登录用户
        UserDo userDo = BeanUtil.toBean(requestParam, UserDo.class);
        LambdaUpdateWrapper<UserDo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserDo::getUsername,requestParam.getUsername());
        baseMapper.update(userDo,updateWrapper);
    }

    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        if (stringRedisTemplate.hasKey(RedisCacheConstant.USER_LOGIN_PREFIX +requestParam.getUsername())){
            throw new ClientException(UserErrorCodeEnum.USER_ALREADY_LOGIN);
        }
        LambdaQueryWrapper<UserDo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(UserDo::getUsername,requestParam.getUsername())
                .eq(UserDo::getPassword,requestParam.getPassword())
                .eq(UserDo::getDelFlag,0);
        UserDo userDo = baseMapper.selectOne(lambdaQueryWrapper);
        if (userDo == null){
            throw new ClientException(UserErrorCodeEnum.USER_NULL);
        }
        String uuid = UUID.randomUUID().toString();
//        stringRedisTemplate.opsForValue().set(uuid, JSON.toJSONString(userDo),30L, TimeUnit.MINUTES);
        stringRedisTemplate.opsForHash().put(RedisCacheConstant.USER_LOGIN_PREFIX + requestParam.getUsername(),uuid,JSON.toJSONString(userDo));
        stringRedisTemplate.expire(RedisCacheConstant.USER_LOGIN_PREFIX + requestParam.getUsername(),30L, TimeUnit.MINUTES);
        return new UserLoginRespDTO(uuid);

    }

    @Override
    public Boolean checkLogin(String username, String token) {
        return stringRedisTemplate.opsForHash().get(RedisCacheConstant.USER_LOGIN_PREFIX + username, token) != null ;
    }

}
