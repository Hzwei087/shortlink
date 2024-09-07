package com.nageoffer.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.admin.common.biz.user.UserContext;
import com.nageoffer.shortlink.admin.common.constant.RedisCacheConstant;
import com.nageoffer.shortlink.admin.common.convention.exception.ClientException;
import com.nageoffer.shortlink.admin.common.convention.exception.ServiceException;
import com.nageoffer.shortlink.admin.common.convention.result.Result;
import com.nageoffer.shortlink.admin.dao.entity.GroupDO;
import com.nageoffer.shortlink.admin.dao.entity.GroupUniqueDO;
import com.nageoffer.shortlink.admin.dao.mapper.GroupMapper;
import com.nageoffer.shortlink.admin.dao.mapper.GroupUniqueMapper;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkGroupSortReqDTO;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkGroupUpadteReqDTO;
import com.nageoffer.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import com.nageoffer.shortlink.admin.remote.ShortLinkActualRemoteService;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.nageoffer.shortlink.admin.service.GroupService;
import com.nageoffer.shortlink.admin.utils.RandomGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {

    @Value("${short-link.group.max-num}")
    private Integer groupMaxNum;

    private final ShortLinkActualRemoteService shortLinkActualRemoteService;
    private final RedissonClient redissonClient;
    private final GroupUniqueMapper groupUniqueMapper;
    private final RBloomFilter<String> gidRegisterCachePenetrationBloomFilter;


    @Override
    public void saveGroup(String groupName) {
        saveGroup(UserContext.getUsername(),groupName);
    }

    @Override
    public void saveGroup(String username, String groupName) {
        RLock lock = redissonClient.getLock(String.format(RedisCacheConstant.LOCK_GROUP_CREATE_KEY, username));
        lock.lock();
        try {
            LambdaQueryWrapper<GroupDO> groupSizeQueryWrapper = new LambdaQueryWrapper<>();
            groupSizeQueryWrapper.eq(GroupDO::getUsername,username);
            groupSizeQueryWrapper.eq(GroupDO::getDelFlag,0);
            List<GroupDO> groupDOList = baseMapper.selectList(groupSizeQueryWrapper);
            if(CollUtil.isNotEmpty(groupDOList) && groupDOList.size() == groupMaxNum){
                throw new ClientException(String.format("已超出最大分组数：%d", groupMaxNum));
            }
            int retryCount = 0;
            int maxRetries = 10;
            String gid = null;
            // 如果布隆过滤器满了，可能会一直重复，所以这里加一个限制条件
            while (retryCount < maxRetries) {
                gid = saveGroupUniqueReturnGid();
                if (StrUtil.isNotEmpty(gid)) {
                    GroupDO groupDO = GroupDO.builder()
                            .gid(gid)
                            .sortOrder(0)
                            .username(username)
                            .name(groupName)
                            .build();
                    baseMapper.insert(groupDO);
                    gidRegisterCachePenetrationBloomFilter.add(gid);
                    break;
                }
                retryCount++;
            }
            if (StrUtil.isEmpty(gid)) {
                throw new ServiceException("生成分组标识频繁");
            }

        }finally {
            lock.unlock();
        }
    }

    private String saveGroupUniqueReturnGid() {
        String gid = RandomGenerator.generateRandom();
        if (gidRegisterCachePenetrationBloomFilter.contains(gid)) {
            return null;
        }
        GroupUniqueDO groupUniqueDO = GroupUniqueDO.builder()
                .gid(gid)
                .build();
        try {
            // 线程 A 和 B 同时生成了相同的 Gid，会被数据库的唯一索引校验触发异常
            // 流程不能被这个异常阻断，需要获取异常重试
            groupUniqueMapper.insert(groupUniqueDO);
        } catch (DuplicateKeyException e) {
            return null;
        }
        return gid;
    }

    @Override
    public List<ShortLinkGroupRespDTO> listGroup() {
        // 创建查询条件
        LambdaQueryWrapper<GroupDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getDelFlag, 0)
                .orderByDesc(GroupDO::getSortOrder, GroupDO::getUpdateTime);

        // 查询数据库
        List<GroupDO> groupDOList = baseMapper.selectList(queryWrapper);

        // 获取短链接数量列表
        List<String> groupIds = groupDOList.stream()//将 groupDOList（一个 List<GroupDO>）转换为一个流（Stream）。流是一种支持顺序和并行聚合操作的数据视图。
                .map(GroupDO::getGid)//使用 map 方法将流中的每个 GroupDO 对象转换为其 gid。 GroupDO::getGid 是一个方法引用，表示对流中的每个 GroupDO 对象调用 getGid() 方法。这会生成一个新的流，其中包含了原始流中每个 GroupDO 对象的 gid
                .collect(Collectors.toList());//使用 collect 方法将流中的元素收集到一个 List 中。Collectors.toList() 是一个收集器，表示将流中的元素收集到一个新的 List 中。
        Result<List<ShortLinkGroupCountQueryRespDTO>> listResult = shortLinkActualRemoteService.listGroupShortLinkCount(groupIds);

        // 转换 listResult 为 Map 以提高查找效率
        Map<String, Integer> shortLinkCountMap = listResult.getData().stream()
                .collect(Collectors.toMap(ShortLinkGroupCountQueryRespDTO::getGid,
                        ShortLinkGroupCountQueryRespDTO::getShortLinkCount));

        // 复制数据并设置短链接数量
        List<ShortLinkGroupRespDTO> shortLinkGroupRespDTOList = BeanUtil.copyToList(groupDOList, ShortLinkGroupRespDTO.class);
        shortLinkGroupRespDTOList.forEach(each -> {
            Integer count = shortLinkCountMap.get(each.getGid());
            if (count != null) {
                each.setShortLinkCount(count);
            }
        });

        // 返回结果
        return shortLinkGroupRespDTOList;
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
        //软删除
        LambdaUpdateWrapper<GroupDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(GroupDO::getGid,gid);
        updateWrapper.eq(GroupDO::getUsername,UserContext.getUsername());
        updateWrapper.eq(GroupDO::getDelFlag,0);
        updateWrapper.set(GroupDO::getDelFlag,1);
        this.update(updateWrapper);
    }
}
