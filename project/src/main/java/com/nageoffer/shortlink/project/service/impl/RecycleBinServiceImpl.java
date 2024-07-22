package com.nageoffer.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.project.common.constant.RedisKeyConstant;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkDO;
import com.nageoffer.shortlink.project.dao.mapper.ShortLinkMapper;
import com.nageoffer.shortlink.project.dto.req.RecycleBinRecoverReqDTO;
import com.nageoffer.shortlink.project.dto.req.RecycleBinRemoveReqDTO;
import com.nageoffer.shortlink.project.dto.req.RecycleBinSaveReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.nageoffer.shortlink.project.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class RecycleBinServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements RecycleBinService {
    private final StringRedisTemplate redisTemplate;
    @Override
    public void saveRecycleBin(RecycleBinSaveReqDTO requestParam) {
        LambdaUpdateWrapper<ShortLinkDO> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getEnableStatus,0)
                .eq(ShortLinkDO::getDelFlag,0);
        ShortLinkDO shortLinkDO = new ShortLinkDO();
        shortLinkDO.setEnableStatus(1);
        baseMapper.update(shortLinkDO,lambdaUpdateWrapper);
        redisTemplate.delete(String.format(RedisKeyConstant.GOTO_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageRecycleShortLink(ShortLinkRecycleBinPageReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .in(ShortLinkDO::getGid, requestParam.getGidList())//在该用户的所有gid的集合范围内
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 1)
                .orderByDesc(ShortLinkDO::getUpdateTime);
        IPage<ShortLinkDO> resultPage = baseMapper.selectPage(requestParam, queryWrapper);
        return resultPage.convert(each -> BeanUtil.toBean(each, ShortLinkPageRespDTO.class));
    }

    @Override
    public void recoverRecycleBin(RecycleBinRecoverReqDTO requestParam) {
        LambdaUpdateWrapper<ShortLinkDO> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getEnableStatus,1)
                .eq(ShortLinkDO::getDelFlag,0);
//        ShortLinkDO selectshortLinkDO = baseMapper.selectOne(lambdaUpdateWrapper);

        ShortLinkDO shortLinkDO = new ShortLinkDO();
        shortLinkDO.setEnableStatus(0);
        baseMapper.update(shortLinkDO,lambdaUpdateWrapper);
        //可不做缓存预热，回收站恢复的短链接访问量不会很高，走跳转业务逻辑里面的缓存预热即可
//        redisTemplate.opsForValue()
//                .set((String.format(RedisKeyConstant.GOTO_SHORT_LINK_KEY, requestParam.getFullShortUrl())),
//                        selectshortLinkDO.getOriginUrl(),
//                        LinkUtil.getLinkCacheValidDate(selectshortLinkDO.getValidDate()), TimeUnit.MILLISECONDS);
        redisTemplate.delete(String.format(RedisKeyConstant.GOTO_IS_NULL_SHORT_LINK_KEY, requestParam.getFullShortUrl()));


    }

    @Override
    public void removeRecycleBin(RecycleBinRemoveReqDTO requestParam) {
        LambdaUpdateWrapper<ShortLinkDO> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getEnableStatus,1)
                .eq(ShortLinkDO::getDelFlag,0);
        ShortLinkDO shortLinkDO = new ShortLinkDO();
        shortLinkDO.setDelFlag(1);
        baseMapper.update(shortLinkDO,lambdaUpdateWrapper);

    }
}
