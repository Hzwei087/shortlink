package com.nageoffer.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.admin.common.biz.user.UserContext;
import com.nageoffer.shortlink.admin.common.constant.RedisCacheConstant;
import com.nageoffer.shortlink.admin.common.convention.exception.ClientException;
import com.nageoffer.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.nageoffer.shortlink.admin.dao.entity.UserDO;
import com.nageoffer.shortlink.admin.dao.mapper.UserMapper;
import com.nageoffer.shortlink.admin.dto.req.UserLoginReqDTO;
import com.nageoffer.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.nageoffer.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserRespDTO;
import com.nageoffer.shortlink.admin.service.GroupService;
import com.nageoffer.shortlink.admin.service.UserService;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.nageoffer.shortlink.admin.common.constant.RedisCacheConstant.LOCK_USER_REGISTER_KEY;
import static com.nageoffer.shortlink.admin.common.constant.RedisCacheConstant.USER_LOGIN_KEY;
import static com.nageoffer.shortlink.admin.common.enums.UserErrorCodeEnum.USER_NAME_EXIST;

/**
 * 用户接口实现层
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {
    @Autowired
    private RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private GroupService groupService;

    @Override
    public UserRespDTO getUserByUsername(String username) {
        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserDO::getUsername,username);
        UserDO userDo = baseMapper.selectOne(queryWrapper);
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
        if (!lock.tryLock()) {
            throw new ClientException(USER_NAME_EXIST);
        }


        try {
            int insert = baseMapper.insert(BeanUtil.toBean(requestParam, UserDO.class));
            if (insert < 1) {
                throw new ClientException(UserErrorCodeEnum.USER_SAVE_ERROR);
            }
            userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());
            groupService.saveGroup(requestParam.getUsername(), "默认分组");
        } catch (DuplicateKeyException e) {
            throw new ClientException(USER_NAME_EXIST);
        } finally {
            lock.unlock();
        }




    }

    @Override
    public void updateByReqDTO(UserUpdateReqDTO requestParam) {
        if (!Objects.equals(requestParam.getUsername(), UserContext.getUsername())){
            throw new ClientException("当前登录用户修改请求异常");

        }
        UserDO userDo = BeanUtil.toBean(requestParam, UserDO.class);
        LambdaUpdateWrapper<UserDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserDO::getUsername,requestParam.getUsername());
        baseMapper.update(userDo,updateWrapper);
    }

    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        LambdaQueryWrapper<UserDO> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(UserDO::getUsername,requestParam.getUsername())
                .eq(UserDO::getPassword,requestParam.getPassword())
                .eq(UserDO::getDelFlag,0);
        UserDO userDo = baseMapper.selectOne(lambdaQueryWrapper);
        if (userDo == null){
            throw new ClientException(UserErrorCodeEnum.USER_NULL);
        }
        //仅允许用户单端登录
//        Boolean hasLogin = stringRedisTemplate.hasKey(RedisCacheConstant.USER_LOGIN_PREFIX + requestParam.getUsername());
//        if (hasLogin && hasLogin != null){
//            throw new ClientException(UserErrorCodeEnum.USER_ALREADY_LOGIN);
//        }
        //可允许同一用户多端登录
        Map<Object, Object> hasLoginMap = stringRedisTemplate.opsForHash().entries(USER_LOGIN_KEY + requestParam.getUsername());
        //Key是USER_LOGIN_KEY + requestParam.getUsername(), hashkey是uudi, value是userdo的json序列
        //entries(...)方法返回存储在该键下的所有字段和值，作为一个Map<Object, Object>对象。
        if (CollUtil.isNotEmpty(hasLoginMap)) {
            stringRedisTemplate.expire(USER_LOGIN_KEY + requestParam.getUsername(), 30L, TimeUnit.MINUTES);
            //将hasLoginMap中的键集合转换为流（stream）
            String token = hasLoginMap.keySet().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElseThrow(() -> new ClientException("用户登录错误"));
            return new UserLoginRespDTO(token);
        }
        /**
         * Hash
         * Key：login_用户名
         * Value：
         *  Key：token标识
         *  Val：JSON 字符串（用户信息）
         */

        String uuid = UUID.randomUUID().toString();
//        stringRedisTemplate.opsForValue().set(uuid, JSON.toJSONString(userDo),30L, TimeUnit.MINUTES);
        stringRedisTemplate.opsForHash().put(RedisCacheConstant.USER_LOGIN_KEY + requestParam.getUsername(),uuid,JSON.toJSONString(userDo));
        stringRedisTemplate.expire(RedisCacheConstant.USER_LOGIN_KEY + requestParam.getUsername(),30L, TimeUnit.MINUTES);
        return new UserLoginRespDTO(uuid);

    }

    @Override
    public Boolean checkLogin(String username, String token) {
        return stringRedisTemplate.opsForHash().get(RedisCacheConstant.USER_LOGIN_KEY + username, token) != null ;
    }

    @Override
    public void logOut(String username, String token) {
        if(checkLogin(username, token)){
            stringRedisTemplate.delete(RedisCacheConstant.USER_LOGIN_KEY + username);
            return;
        }else {
            throw new ClientException("用户Token不存在或用户未登录");
        }

    }

}
