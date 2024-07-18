package com.nageoffer.shortlink.project.dto.req;

import lombok.Data;

import java.util.Date;

@Data
/**
 * 创建短链接请求对象
 */
public class ShortLinkCreateReqDTO {
    /**
     * 域名
     */
    private String domain;

    /**
     * 原始链接
     */
    private String origin_url;

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 创建类型 0 接口创建 1控制台创建
     */
    private int createdType;

    /**
     * 有效期类型 0 永久有效 1临时有效
     */
    private int validDateType;

    /**
     * 有效期
     */
    private Date validDate;

    /**
     * 描述
     */
    private String describe;

}