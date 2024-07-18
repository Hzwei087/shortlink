package com.nageoffer.shortlink.admin.dto.resp;

import lombok.Data;

/**
 * 短链接分组返回实体
 */
@Data
public class ShortLinkGroupRespDTO {
    /**
     * 分组标识
     */
    private String gid;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 用户名
     */
    private String username;
    /**
     * 排序优先级
     */
    private Integer sortOrder;
}