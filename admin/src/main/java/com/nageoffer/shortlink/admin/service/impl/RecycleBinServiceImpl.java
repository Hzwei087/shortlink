package com.nageoffer.shortlink.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nageoffer.shortlink.admin.common.biz.user.UserContext;
import com.nageoffer.shortlink.admin.common.convention.exception.ClientException;
import com.nageoffer.shortlink.admin.common.convention.exception.ServiceException;
import com.nageoffer.shortlink.admin.common.convention.result.Result;
import com.nageoffer.shortlink.admin.dao.entity.GroupDO;
import com.nageoffer.shortlink.admin.dao.mapper.GroupMapper;
import com.nageoffer.shortlink.admin.remote.dto.ShortLinkRemoteService;
import com.nageoffer.shortlink.admin.remote.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import com.nageoffer.shortlink.admin.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecycleBinServiceImpl implements RecycleBinService {
    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService() {
    };
    private final GroupMapper groupMapper;

    @Override
    public Result<IPage<ShortLinkPageRespDTO>> pageRecycleShortLink(ShortLinkRecycleBinPageReqDTO requestParam) {
        LambdaQueryWrapper<GroupDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getDelFlag,0);
        List<GroupDO> groupDOList = groupMapper.selectList(queryWrapper);
        if (CollUtil.isEmpty(groupDOList)){
            throw new ServiceException("用户无分组信息");
        }
        List<String> list = groupDOList.stream().map(GroupDO::getGid).toList();
        requestParam.setGidList(list);
        return shortLinkRemoteService.pageRecycleBinShortLink(requestParam);
    }
}
