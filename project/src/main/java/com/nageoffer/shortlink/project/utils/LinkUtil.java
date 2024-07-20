package com.nageoffer.shortlink.project.utils;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.nageoffer.shortlink.project.common.constant.ShortLinkConstant;
import lombok.Data;

import java.util.Date;
import java.util.Optional;

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
}
