package com.nageoffer.shortlink.admin.remote.dto;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nageoffer.shortlink.admin.common.convention.result.Result;
import com.nageoffer.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import org.apache.commons.collections4.map.HashedMap;

import java.util.Map;

public interface ShortLinkRemoteService {
    default Result<ShortLinkCreateRespDTO> creatShortLink(ShortLinkCreateReqDTO requestParam){

        String resultBodyStr = HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/create",JSON.toJSONString(requestParam));
        return JSON.parseObject(resultBodyStr, new TypeReference<Result<ShortLinkCreateRespDTO>>() {
        });
    };

    default Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam){
        Map<String, Object> requestMap = new HashedMap<>();
        requestMap.put("gid", requestParam.getGid());
        requestMap.put("size",requestParam.getSize());
        requestMap.put("current", requestParam.getCurrent());
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/page",requestMap);

        return JSON.parseObject(resultPageStr, new TypeReference<Result<IPage<ShortLinkPageRespDTO>>>() {
        });
    };
}
