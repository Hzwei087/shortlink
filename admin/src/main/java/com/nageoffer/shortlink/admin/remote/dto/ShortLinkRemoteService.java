package com.nageoffer.shortlink.admin.remote.dto;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nageoffer.shortlink.admin.common.convention.result.Result;
import com.nageoffer.shortlink.admin.dto.req.RecycleBinRemoveReqDTO;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkStatsReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.req.*;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkStatsRespDTO;
import org.apache.commons.collections4.map.HashedMap;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

public interface ShortLinkRemoteService {
    /**
     * 创建短链接
     * @param requestParam
     * @return
     */
    default Result<ShortLinkCreateRespDTO> creatShortLink(ShortLinkCreateReqDTO requestParam){

        String resultBodyStr = HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/create",JSON.toJSONString(requestParam));
        return JSON.parseObject(resultBodyStr, new TypeReference<Result<ShortLinkCreateRespDTO>>() {
        });
    };

    /**
     * 分页查询短链接
     * @param requestParam
     * @return
     */
    default Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam){
        Map<String, Object> requestMap = new HashedMap<>();
        requestMap.put("gid", requestParam.getGid());
        requestMap.put("size",requestParam.getSize());
        requestMap.put("current", requestParam.getCurrent());
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/page",requestMap);

        return JSON.parseObject(resultPageStr, new TypeReference<Result<IPage<ShortLinkPageRespDTO>>>() {
        });
    };
    /**
     * 查询分组内短链接数量
     */

    default Result<List<ShortLinkGroupCountQueryRespDTO>> listGroupShortLinkCount(List<String> requestParam){
        Map<String, Object> requestMap = new HashedMap<>();
        requestMap.put("requestParam", requestParam);
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/count",requestMap);

        return JSON.parseObject(resultPageStr, new TypeReference<Result<List<ShortLinkGroupCountQueryRespDTO>>>() {
        });
    };
    /**
     * 短链接信息修改
     */
    default void updateShortLink(ShortLinkUpdateReqDTO requestParam){
        String resultBodyStr = HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/update",JSON.toJSONString(requestParam));
    }

    default Result<String> getTitleByUrl(String url){
        String resultStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/title?url=" + url);
        return JSON.parseObject(resultStr, new TypeReference<Result<String>>(){});
    }
    /**
     * 短链接转移入回收站
     */
    default void saveRecycleBin(RecycleBinSaveReqDTO requestParam){
        String resultBodyStr = HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/recycle-bin/save",JSON.toJSONString(requestParam));
    }
    /**
     * 分页查询回收站中的短链接
     */
    default  Result<IPage<ShortLinkPageRespDTO>> pageRecycleBinShortLink(ShortLinkRecycleBinPageReqDTO requestParam){
        Map<String, Object> requestMap = new HashedMap<>();
        requestMap.put("gidList",requestParam.getGidList());
        requestMap.put("size",requestParam.getSize());
        requestMap.put("current", requestParam.getCurrent());
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/recycle-bin/page",requestMap);

        return JSON.parseObject(resultPageStr, new TypeReference<Result<IPage<ShortLinkPageRespDTO>>>() {
        });
    };
    /**
     * 恢复回收站中的短链接
     */
    default void recoverRecycleBin(RecycleBinRecoverReqDTO requestParam){
        String resultBodyStr = HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/recycle-bin/recover",JSON.toJSONString(requestParam));
    }


    default void removeRecycleBin(RecycleBinRemoveReqDTO requestParam){
        String resultBodyStr = HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/recycle-bin/remove",JSON.toJSONString(requestParam));
    };

    /**
     * 访问单个短链接指定时间内监控数据
     */
    default Result<ShortLinkStatsRespDTO> shortLinkStats(ShortLinkStatsReqDTO requestParam) {
        Map<String, Object> requestMap = new HashedMap<>();
        requestMap.put("fullShortUrl",requestParam.getFullShortUrl());
        requestMap.put("gid",requestParam.getGid());
        requestMap.put("startDate", requestParam.getStartDate());
        requestMap.put("endDate", requestParam.getEndDate());
        requestMap.put("enableStatus", requestParam.getEnableStatus());
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/stats",requestMap);

        return JSON.parseObject(resultPageStr, new TypeReference<Result<ShortLinkStatsRespDTO>>() {
        });
    }
}
