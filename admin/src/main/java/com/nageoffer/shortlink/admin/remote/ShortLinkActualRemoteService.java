package com.nageoffer.shortlink.admin.remote;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nageoffer.shortlink.admin.common.convention.result.Result;
import com.nageoffer.shortlink.admin.dto.req.RecycleBinRecoverReqDTO;
import com.nageoffer.shortlink.admin.dto.req.RecycleBinRemoveReqDTO;
import com.nageoffer.shortlink.admin.dto.req.RecycleBinSaveReqDTO;
import com.nageoffer.shortlink.admin.dto.resp.ShortLinkBatchCreateRespDTO;
import com.nageoffer.shortlink.admin.remote.dto.req.*;
import com.nageoffer.shortlink.admin.remote.dto.resp.*;
import org.apache.commons.collections4.map.HashedMap;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 短链接中台远程调用服务
 */
@FeignClient("short-link-project")
public interface ShortLinkActualRemoteService {
    /**
     * 创建短链接
     * @param requestParam
     * @return
     */
    @PostMapping("/api/short-link/v1/create")
    Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam);

    /**
     * 批量创建短链接
     */
    @PostMapping("/api/short-link/v1/create/batch")
    Result<ShortLinkBatchCreateRespDTO> batchCreateShortLink(@RequestBody ShortLinkBatchCreateReqDTO requestParam);

    /**
     * 分页查询短链接
     * @param gid
     * @param orderTag
     * @param current
     * @param size
     * @return
     */
    @GetMapping("/api/short-link/v1/page")
    Result<Page<ShortLinkPageRespDTO>> pageShortLink(@RequestParam("gid") String gid,
                                                     @RequestParam("orderTag") String orderTag,
                                                     @RequestParam("current") Long current,
                                                     @RequestParam("size") Long size);

    /**
     * 查询分组内短链接数量
     */
    @PutMapping("/api/short-link/v1/count")
    Result<List<ShortLinkGroupCountQueryRespDTO>> listGroupShortLinkCount(@RequestParam("requestParam") List<String> requestParam);

    /**
     * 短链接信息修改
     */
    @PostMapping("/api/short-link/v1/update")
    void updateShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam);

    /**
     * 根据链接获取标题
     * @param url
     * @return
     */
    @GetMapping("/api/short-link/v1/title")
    Result<String> getTitleByUrl(@RequestParam("url") String url);

    /**
     * 短链接转移入回收站
     */
    @PostMapping("/api/short-link/v1/recycle-bin/save")
    void saveRecycleBin(@RequestBody RecycleBinSaveReqDTO requestParam);

    /**
     * 分页查询回收站中的短链接
     */
    @GetMapping("/api/short-link/v1/recycle-bin/page")
    Result<Page<ShortLinkPageRespDTO>> pageRecycleBinShortLink(@RequestParam("gidList") List<String> gidList,
                                                                @RequestParam("size") Long size,
                                                                @RequestParam("current") Long current);
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

    /**
     * 访问分组内所有短链接指定时间内监控数据
     */
    default Result<ShortLinkStatsRespDTO> groupShortLinkStats(ShortLinkGroupStatsReqDTO requestParam){
        Map<String, Object> requestMap = new HashedMap<>();
        requestMap.put("gid",requestParam.getGid());
        requestMap.put("startDate", requestParam.getStartDate());
        requestMap.put("endDate", requestParam.getEndDate());
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/stats/group",requestMap);

        return JSON.parseObject(resultPageStr, new TypeReference<Result<ShortLinkStatsRespDTO>>() {
        });
    };

    /**
     * 访问单个短链接指定时间内访问日志
     */
    default Result<IPage<ShortLinkStatsAccessRecordRespDTO>> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam) {
        Map<String, Object> requestMap = new HashedMap<>();
        requestMap.put("size", requestParam.getSize());
        requestMap.put("current", requestParam.getCurrent());
        requestMap.put("fullShortUrl", requestParam.getFullShortUrl());
        requestMap.put("gid", requestParam.getGid());
        requestMap.put("startDate", requestParam.getStartDate());
        requestMap.put("endDate", requestParam.getEndDate());
        requestMap.put("enableStatus", requestParam.getEnableStatus());
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/stats/access-record", requestMap);

        return JSON.parseObject(resultPageStr, new TypeReference<Result<IPage<ShortLinkStatsAccessRecordRespDTO>>>() {
        });
    }
    /**
     * 分页查询分组内所有短链接指定时间内访问日志
     */
    default Result<IPage<ShortLinkStatsAccessRecordRespDTO>> groupShortLinkStatsAccessRecord(ShortLinkGroupStatsAccessRecordReqDTO requestParam){
        Map<String, Object> requestMap = new HashedMap<>();
        requestMap.put("size", requestParam.getSize());
        requestMap.put("current", requestParam.getCurrent());
        requestMap.put("gid", requestParam.getGid());
        requestMap.put("startDate", requestParam.getStartDate());
        requestMap.put("endDate", requestParam.getEndDate());
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/stats/access-record/group", requestMap);

        return JSON.parseObject(resultPageStr, new TypeReference<Result<IPage<ShortLinkStatsAccessRecordRespDTO>>>() {
        });
    };


}
