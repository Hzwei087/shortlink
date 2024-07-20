package com.nageoffer.shortlink.admin.remote.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShortLinkGroupCountQueryRespDTO {
    /**
     * 分组标识
     */
    String gid;
    /**
     * 短链接数量
     */
    Integer shortLinkCount;
}
