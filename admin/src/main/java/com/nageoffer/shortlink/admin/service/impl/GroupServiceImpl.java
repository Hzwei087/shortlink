package com.nageoffer.shortlink.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.admin.common.convention.exception.ClientException;
import com.nageoffer.shortlink.admin.common.enums.GroupErrorCodeEnum;
import com.nageoffer.shortlink.admin.dao.entity.GroupDO;
import com.nageoffer.shortlink.admin.dao.mapper.GroupMapper;
import com.nageoffer.shortlink.admin.service.GroupService;
import com.nageoffer.shortlink.admin.utils.RandomStringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {
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
                .eq(GroupDO::getUsername,null);
        while (baseMapper.exists(gidQueryWrapper)) {
            gid = RandomStringUtil.generateRandom();
            gidQueryWrapper.eq(GroupDO::getGid, gid);
        }

        GroupDO groupDO = GroupDO.builder()
                .gid(gid)
                .name(groupName)
                .build();
        baseMapper.insert(groupDO);

    }
}
