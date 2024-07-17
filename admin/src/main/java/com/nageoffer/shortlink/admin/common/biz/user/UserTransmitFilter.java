package com.nageoffer.shortlink.admin.common.biz.user;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.nageoffer.shortlink.admin.common.constant.RedisCacheConstant;
import com.nageoffer.shortlink.admin.common.convention.exception.ClientException;
import com.nageoffer.shortlink.admin.common.convention.result.Results;
import com.nageoffer.shortlink.admin.common.enums.UserErrorCodeEnum;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;

/**
 * 用户信息传输过滤器
 */
@RequiredArgsConstructor
public class UserTransmitFilter implements Filter {
    private final StringRedisTemplate stringRedisTemplate;

    private static final List<String> IGNOGE_URI = Lists.newArrayList(
            "/api/short-link/admin/v1/user/login",
            "/api/short-link/admin/v1/user/has-username"
    );
    //"/api/short-link/admin/v1/user" 注册，修改都是用这个路径，用了RESTful来区别，所以没办法用路径来放行

    @SneakyThrows
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String requestURI = httpServletRequest.getRequestURI();
        //放行忽略拦截的路径
        if(!IGNOGE_URI.contains(requestURI)){
            String method = httpServletRequest.getMethod();
            //如果是/api/short-link/admin/v1/user/has-username路径同时请求方法为POST，不需要验证Token
            boolean check = !(Objects.equals(requestURI,"/api/short-link/admin/v1/user") && Objects.equals(method,"POST"));
            if(check){
                String username = httpServletRequest.getHeader("username");
                String token = httpServletRequest.getHeader("token");
                if(!StrUtil.isAllNotBlank(username,token)){
                    returnJason((HttpServletResponse) servletResponse,JSON.toJSONString(Results.failure(new ClientException(UserErrorCodeEnum.USER_TOKEN_FAIL))));
                    return;
                }
                Object userInfoJasonStr;
                try {
                    userInfoJasonStr = stringRedisTemplate.opsForHash().get(RedisCacheConstant.USER_LOGIN_PREFIX + username,token);
                    if (userInfoJasonStr == null){
//                        throw new ClientException(UserErrorCodeEnum.USER_TOKEN_FAIL);
                        returnJason((HttpServletResponse) servletResponse,JSON.toJSONString(Results.failure(new ClientException(UserErrorCodeEnum.USER_TOKEN_FAIL))));
                        return;
                    }
                }catch (Exception e){
//                    throw new ClientException(UserErrorCodeEnum.USER_TOKEN_FAIL);
                    returnJason((HttpServletResponse) servletResponse,JSON.toJSONString(Results.failure(new ClientException(UserErrorCodeEnum.USER_TOKEN_FAIL))));
                    return;

                }
                UserInfoDTO userInfoDTO = JSON.parseObject(userInfoJasonStr.toString(), UserInfoDTO.class);
                UserContext.setUser(userInfoDTO);
            }
        }

        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            UserContext.removeUser();
        }
    }

    private void returnJason(HttpServletResponse response, String jason) throws Exception {
        PrintWriter writer = null;
        response.setCharacterEncoding("UTF-8");
        try {
            writer = response.getWriter();
            writer.print(jason);
        } catch (IOException e) {
            if (writer != null) {
                writer.close();
            }
        }
    }

}

