package com.nageoffer.shortlink.admin.dto.req;

import lombok.Data;

@Data
public class ShortLinkGroupUpadteReqDTO {
    /**
     * 分组标识
     */
    private String gid;
    /**
     * 排序字段
      */
    private String name;

}
