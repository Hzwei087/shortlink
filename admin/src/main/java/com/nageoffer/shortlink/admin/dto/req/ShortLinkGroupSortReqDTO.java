package com.nageoffer.shortlink.admin.dto.req;

import lombok.Data;

@Data
public class ShortLinkGroupSortReqDTO {
    /**
     * 分组标识
     */
    private String gid;
    /**
     * 分组名
     */
    private Integer sortOrder;

}
