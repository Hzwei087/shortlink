package com.nageoffer.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.admin.common.biz.user.UserContext;
import com.nageoffer.shortlink.admin.common.convention.exception.ClientException;
import com.nageoffer.shortlink.admin.common.enums.GroupErrorCodeEnum;
import com.nageoffer.shortlink.admin.dao.entity.GroupDO;
import com.nageoffer.shortlink.admin.dao.mapper.GroupMapper;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkGroupSortReqDTO;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkGroupUpadteReqDTO;
import com.nageoffer.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import com.nageoffer.shortlink.admin.service.GroupService;
import com.nageoffer.shortlink.admin.utils.RandomStringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;


@Slf4j
@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {

    @Autowired
    private GroupMapper groupMapper;

    @Override
    public void saveGroup(String groupName) {

        LambdaQueryWrapper<GroupDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupDO::getName, groupName);
        if (baseMapper.exists(queryWrapper)) {
            throw new ClientException(GroupErrorCodeEnum.GROUP_NAME_EXIST);
        }

        String gid = RandomStringUtil.generateRandom();
        LambdaQueryWrapper<GroupDO> gidQueryWrapper = new LambdaQueryWrapper<>();
        gidQueryWrapper.eq(GroupDO::getGid, gid)
                // TODO 设置用户名
                .eq(GroupDO::getUsername,UserContext.getUsername());
        while (baseMapper.exists(gidQueryWrapper)) {
            gid = RandomStringUtil.generateRandom();
            gidQueryWrapper.eq(GroupDO::getGid, gid);
        }

        GroupDO groupDO = GroupDO.builder()
                .gid(gid)
                .name(groupName)
                .username(UserContext.getUsername())
                .sortOrder(0)
                .build();
        baseMapper.insert(groupDO);

    }

    @Override
    public List<ShortLinkGroupRespDTO> listGroup() {
        // TODO 从当前上下文获取用户名
        LambdaQueryWrapper<GroupDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getDelFlag,0)
                .orderByDesc(GroupDO::getSortOrder,GroupDO::getUpdateTime);
        List<GroupDO> groupDOList = baseMapper.selectList(queryWrapper);

        return BeanUtil.copyToList(groupDOList, ShortLinkGroupRespDTO.class);
    }

    @Override
    public void updateGroup(ShortLinkGroupUpadteReqDTO requestParam) {
        LambdaUpdateWrapper<GroupDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(GroupDO::getGid,requestParam.getGid());
        updateWrapper.eq(GroupDO::getUsername,UserContext.getUsername());
        updateWrapper.eq(GroupDO::getDelFlag,0);
        updateWrapper.set(GroupDO::getName,requestParam.getName());
        this.update(updateWrapper);
    }

    @Override
    public void sortGroup(List<ShortLinkGroupSortReqDTO> requestParam) {
        Iterator<ShortLinkGroupSortReqDTO> iterator = requestParam.iterator();
        while (iterator.hasNext()) {
            ShortLinkGroupSortReqDTO next =  (ShortLinkGroupSortReqDTO)iterator.next();
            LambdaUpdateWrapper<GroupDO> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(GroupDO::getDelFlag,0)
                    .eq(GroupDO::getGid,next.getGid())
                    .eq(GroupDO::getUsername,UserContext.getUsername())
                    .set(GroupDO::getSortOrder,next.getSortOrder());
            this.update(updateWrapper);
        }

    }

    @Override
    public void deleteGroup(String gid) {
        LambdaUpdateWrapper<GroupDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(GroupDO::getGid,gid);
        updateWrapper.eq(GroupDO::getUsername,UserContext.getUsername());
        updateWrapper.eq(GroupDO::getDelFlag,0);
        updateWrapper.set(GroupDO::getDelFlag,1);
        this.update(updateWrapper);
    }
}
