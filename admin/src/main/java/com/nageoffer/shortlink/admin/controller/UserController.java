package com.nageoffer.shortlink.admin.controller;

import cn.hutool.core.bean.BeanUtil;
import com.nageoffer.shortlink.admin.common.convention.result.Result;
import com.nageoffer.shortlink.admin.common.convention.result.Results;
import com.nageoffer.shortlink.admin.dto.req.UserLoginReqDTO;
import com.nageoffer.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.nageoffer.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserActualRespDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserRespDTO;
import com.nageoffer.shortlink.admin.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制层
 */
@RestController
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 根据用户名查询用户信息
     */
    @GetMapping("/api/short-link/v1/user/{username}")
    public Result<UserRespDTO> getUserByUsername(@PathVariable("username") String username) {
        UserRespDTO userByUsername = userService.getUserByUsername(username);
        return Results.success(userByUsername);
    }

    /**
     * 根据用户名查询无脱敏用户信息
     */
    @GetMapping("/api/short-link/v1/actual/user/{username}")
    public Result<UserActualRespDTO> getActualUserByUsername(@PathVariable("username") String username) {
        UserRespDTO userByUsername = userService.getUserByUsername(username);
        return Results.success(BeanUtil.toBean(userByUsername, UserActualRespDTO.class));
    }

    /**
     * 查询用户名是否存在
     */
    @GetMapping("/api/short-link/v1/user/has-username")
    public Result<Boolean> hasUsername(@RequestParam("username") String username) {
        return Results.success(userService.hasUsername(username));
    }

    /**
     * 注册
     */
    @PostMapping("/api/short-link/v1/user")
    public Result<Void> register(@RequestBody UserRegisterReqDTO requestParam) {
        userService.register(requestParam);
        return Results.success();
    }

    /**
     * 修改用户
     * @param requestParam
     * @return
     */

    @PutMapping("/api/short-link/v1/user")
    public Result<Void> update(@RequestBody UserUpdateReqDTO requestParam) {
        userService.updateByReqDTO(requestParam);
        return Results.success();
    }

    /**
     * 登录用户
     * @param requestParam
     * @return
     */
    @PostMapping("/api/short-link/v1/user/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParam){
        UserLoginRespDTO userLoginRespDTO = userService.login(requestParam);
        return Results.success(userLoginRespDTO);
    }

    /**
     * 检查用户是否登录
     * @param token
     * @return
     */
    @GetMapping("/api/short-link/v1/user/check-login")
    public Result<Boolean> checkLogin(@RequestParam("username") String username, @RequestParam("token") String token){
        Boolean result = userService.checkLogin(username, token);
        return Results.success(result);
    }

    /**
     * 用户退出登录
     * @param username
     * @param token
     * @return
     */
    @DeleteMapping("/api/short-link/v1/user/log-out")
    public Result<Void> logOut(@RequestParam("username") String username, @RequestParam("token") String token){
        userService.logOut(username, token);
        return Results.success();
    }


}
