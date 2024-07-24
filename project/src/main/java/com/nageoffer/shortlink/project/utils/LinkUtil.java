package com.nageoffer.shortlink.project.utils;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.nageoffer.shortlink.project.common.constant.ShortLinkConstant;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Date;

public class LinkUtil {
    /**
     * 获取短链接缓存有效期时间
     * @param validDate 有效期时间
     * @return 有效期时间戳
     */
    public static long getLinkCacheValidDate(Date validDate){
//        return Optional.ofNullable(validDate)
//                .map(each -> DateUtil.between(new Date(), each, DateUnit.MS))
//                .orElse(ShortLinkConstant.DEFAULT_CACHE_VALID_TIME);
        if (validDate == null){
            return ShortLinkConstant.DEFAULT_CACHE_VALID_TIME;
        }else {
            return DateUtil.between(new Date(),validDate,DateUnit.MS);
        }

    }

    /**
     * 获取用户访问所使用的操作系统
     * @param request
     * @return
     */
    public static String getOs(HttpServletRequest request){
        String userAgent = request.getHeader("User-Agent");
        if (userAgent.toLowerCase().contains("windows")){
            return "Windows";
        }else if (userAgent.toLowerCase().contains("mac")){
            return "Mac OS";
        }else if (userAgent.toLowerCase().contains("linux")){
            return "Linux";
        }else if (userAgent.toLowerCase().contains("android")){
            return "Android";
        }else if (userAgent.toLowerCase().contains("iphone") || userAgent.toLowerCase().contains("ipad")){
            return "iOS";
        }else {
            return "Unknown";
        }

    }
    /**
     * 获取用户访问所使用的浏览器
     * @param request
     * @return
     */

    public static String getBrowser(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent.toLowerCase().contains("edg")) {
            return "Mircosoft Egde";
        } else if (userAgent.toLowerCase().contains("chrome")) {
            return "Google Chrome";
        } else if (userAgent.toLowerCase().contains("firefox")) {
            return "Mozilla Firefox";
        } else if (userAgent.toLowerCase().contains("edg")) {
            return "Mircosoft Egde";
        } else if (userAgent.toLowerCase().contains("safari")) {
            return "Apple Safari";
        } else if (userAgent.toLowerCase().contains("opera")) {
            return "Opera";
        } else if (userAgent.toLowerCase().contains("msie")) {
            return "Internet Explorer";
        } else {
            return "Unknown";
        }

    }
}
