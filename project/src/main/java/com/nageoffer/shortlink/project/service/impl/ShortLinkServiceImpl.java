package com.nageoffer.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.project.common.constant.RedisKeyConstant;
import com.nageoffer.shortlink.project.common.constant.ShortLinkConstant;
import com.nageoffer.shortlink.project.common.convention.exception.ClientException;
import com.nageoffer.shortlink.project.common.convention.exception.ServiceException;
import com.nageoffer.shortlink.project.common.enums.ValiDateTypeEnum;
import com.nageoffer.shortlink.project.dao.entity.*;
import com.nageoffer.shortlink.project.dao.mapper.*;
import com.nageoffer.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.nageoffer.shortlink.project.service.ShortLinkService;
import com.nageoffer.shortlink.project.utils.HashUtil;
import com.nageoffer.shortlink.project.utils.LinkUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {
    private final  RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;

    @Value("${short-link.stats.locale.amap-key}")
    private String statsLocalAmapkey;


    @Override
    public ShortLinkCreateRespDTO creatShortLink(ShortLinkCreateReqDTO requestParam) {
        String shortLinkSuffix = generateSuffix(requestParam);
        String fullShortUrl = requestParam.getDomain() + "/" + shortLinkSuffix;
        ShortLinkDO shortLinkDO = BeanUtil.toBean(requestParam, ShortLinkDO.class);
        shortLinkDO.setFullShortUrl(fullShortUrl);
        shortLinkDO.setShortUri(shortLinkSuffix);
        shortLinkDO.setFavicon(getFavicon(requestParam.getOriginUrl()));
        shortLinkDO.setEnableStatus(0);
        ShortLinkGotoDO linkGotoDO = ShortLinkGotoDO.builder()
                .fullShortUrl(fullShortUrl)
                .gid(requestParam.getGid())
                .build();
        try {
            baseMapper.insert(shortLinkDO);
            shortLinkGotoMapper.insert(linkGotoDO);
        } catch (DuplicateKeyException d) {
            log.warn("短链接:{}重复入库", fullShortUrl);
            throw new ServiceException(String.format("短链接:%s 生成重复", fullShortUrl));
//            }

        }
        //缓存预热
        stringRedisTemplate.opsForValue()
                .set(String.format(RedisKeyConstant.GOTO_SHORT_LINK_KEY, fullShortUrl),
                        shortLinkDO.getOriginUrl(),
                        LinkUtil.getLinkCacheValidDate(shortLinkDO.getValidDate()), TimeUnit.MILLISECONDS);
        shortUriCreateCachePenetrationBloomFilter.add(fullShortUrl);

        return ShortLinkCreateRespDTO.builder()
                .fullShortUrl("http://" + shortLinkDO.getFullShortUrl())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .build();
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0)
                .orderByDesc(ShortLinkDO::getCreateTime);
        IPage<ShortLinkDO> resultPage = baseMapper.selectPage(requestParam, queryWrapper);
        return resultPage.convert(each -> BeanUtil.toBean(each, ShortLinkPageRespDTO.class));
    }

    @Override
    public List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(List<String> requestParam) {
        List<ShortLinkGroupCountQueryRespDTO> dtoList = new ArrayList<>();
        Iterator<String> iterator = requestParam.iterator();
        while (iterator.hasNext()) {
            String next = iterator.next();
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper
                    .eq(ShortLinkDO::getGid, next)
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0);
            Long selectCount = baseMapper.selectCount(queryWrapper);
            Integer intValue = selectCount.intValue();
            dtoList.add(ShortLinkGroupCountQueryRespDTO
                    .builder()
                    .gid(next)
                    .shortLinkCount(intValue)
                    .build());
        }
        return dtoList;
    }

    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0);
        ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
        if (hasShortLinkDO == null) {
            throw new ClientException("短链接记录不存在");
        }
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(hasShortLinkDO.getDomain())
                .shortUri(hasShortLinkDO.getShortUri())
                .clickNum(hasShortLinkDO.getClickNum())
                .favicon(hasShortLinkDO.getFavicon())
                .createdType(hasShortLinkDO.getCreatedType())
                .gid(requestParam.getGid())
                .originUrl(requestParam.getOriginUrl())
                .describe(requestParam.getDescribe())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .build();
        if (Objects.equals(hasShortLinkDO.getGid(), requestParam.getGid())) {
            LambdaUpdateWrapper<ShortLinkDO> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
            lambdaUpdateWrapper.eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, requestParam.getGid())
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .set(Objects.equals(requestParam.getValidDateType(), ValiDateTypeEnum.PERMANENT.getType()), ShortLinkDO::getValidDate, null);
            baseMapper.update(shortLinkDO, lambdaUpdateWrapper);
        } else {
            LambdaUpdateWrapper<ShortLinkDO> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
            lambdaUpdateWrapper.eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, hasShortLinkDO.getGid())
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0);
            baseMapper.delete(lambdaUpdateWrapper);
            baseMapper.insert(shortLinkDO);

            //插入完整短链接映射gid路由表
            ShortLinkGotoDO shortLinkGotoDO = new ShortLinkGotoDO();
            shortLinkGotoDO.setGid(requestParam.getGid());
            LambdaUpdateWrapper<ShortLinkGotoDO> gotoUpdateWrapper = new LambdaUpdateWrapper<>();
            gotoUpdateWrapper
                    .eq(ShortLinkGotoDO::getFullShortUrl, requestParam.getFullShortUrl());
            shortLinkGotoMapper.update(shortLinkGotoDO, gotoUpdateWrapper);
        }
    }

    @SneakyThrows
    @Override
    public void restoreUrl(String shortUri, ServletRequest request, ServletResponse response) {

        String serverName = request.getServerName();
        String fullShortUrl = serverName + "/" + shortUri;
        String redisKey = String.format(RedisKeyConstant.GOTO_SHORT_LINK_KEY, fullShortUrl);
        String originLink = stringRedisTemplate.opsForValue().get(redisKey);
        if (StrUtil.isNotBlank(originLink)) {
            shortLinkStats(fullShortUrl,null,request,response);
            ((HttpServletResponse) response).sendRedirect(originLink);
            return;
        }

        //用布隆过滤器解决缓存穿透（用户用不存在的短链接重复调用接口，穿透过缓存查询数据库）
        boolean contains = shortUriCreateCachePenetrationBloomFilter.contains(fullShortUrl);
        if(!contains){
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        //布隆过滤器检查fullShortUrl是否存在，可能会误判存在，实际可能不存在。如果判定会不存在，实际一定不存在
        //误判存在的无效短链接经过路由表查询后若为空，则在缓存中添加查询为空的记录key = RedisKeyConstant.GOTO_IS_NULL_SHORT_LINK_KEY+fullShortUrl,value ="-";
        //此不存在的短链接在下次数据库查询前，会在缓存中判断是否有曾经查询过为空的记录
        String gotoIsNullShortLink = stringRedisTemplate.opsForValue().get(String.format(RedisKeyConstant.GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl));
        if(StrUtil.isNotBlank(gotoIsNullShortLink)){
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }

        String lockKey = String.format(RedisKeyConstant.LOCK_GOTO_SHORT_LINK_KEY, fullShortUrl);
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        //双重判定锁：首先加分布式锁，防止缓存过期之后的大量请求过来的缓存击穿问题；同时再加上一个双重判定锁，可以让只有第一个拿到锁的请求进行缓存重构，之后拿到锁的请求直接查询缓存即可，提高了程序运行效率

        try {
            originLink = stringRedisTemplate.opsForValue().get(redisKey);
            if (StrUtil.isNotBlank(originLink)) {
                shortLinkStats(fullShortUrl,null,request,response);
                ((HttpServletResponse) response).sendRedirect(originLink);
                return;
            }
            //数据库查询路由表
            LambdaQueryWrapper<ShortLinkGotoDO> linkGotoQueryWrapper = new LambdaQueryWrapper<>();
            linkGotoQueryWrapper.eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
            ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoQueryWrapper);
            if (shortLinkGotoDO == null) {
                stringRedisTemplate.opsForValue().set(String.format(RedisKeyConstant.GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl),"-",30, TimeUnit.SECONDS);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }
            //数据库查询短链接源地址
            LambdaQueryWrapper<ShortLinkDO> shortLinkDOqueryWrapper = new LambdaQueryWrapper<>();
            shortLinkDOqueryWrapper.eq(ShortLinkDO::getGid, shortLinkGotoDO.getGid())
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl)
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0);
            ShortLinkDO shortLinkDO = baseMapper.selectOne(shortLinkDOqueryWrapper);
            if (shortLinkDO == null || shortLinkDO.getValidDate().before(new Date())) {
                stringRedisTemplate.opsForValue().set(String.format(RedisKeyConstant.GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "-", 30, TimeUnit.SECONDS);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }
            String originUrl = shortLinkDO.getOriginUrl();
            stringRedisTemplate.opsForValue().set(
                    redisKey,
                    originUrl,
                    LinkUtil.getLinkCacheValidDate(shortLinkDO.getValidDate()),
                    TimeUnit.MILLISECONDS);
            shortLinkStats(fullShortUrl,shortLinkDO.getGid(),request,response);
            ((HttpServletResponse) response).sendRedirect(originUrl);

        } finally {
            lock.unlock();
        }

    }

    private void shortLinkStats(String fullShortUrl, String gid, ServletRequest request, ServletResponse response){
        AtomicBoolean uvFirstFlag = new AtomicBoolean();
        Cookie[] cookies = ((HttpServletRequest) request).getCookies();
        try {
            AtomicReference<String> uv = new AtomicReference<>();
            //在 Java 中，Runnable 是一个函数式接口，它只有一个抽象方法 run。这意味着可以使用 Lambda 表达式来实现该接口，因为 Lambda 表达式本质上是对一个函数式接口的实现。
            Runnable addResponseCookieTask = () -> {
                //匿名内部类
                uv.set(cn.hutool.core.lang.UUID.fastUUID().toString());
                Cookie uvCookie = new Cookie("uv", uv.get());
                uvCookie.setMaxAge(60 * 60 * 24 * 30);
                uvCookie.setPath(StrUtil.sub(fullShortUrl, fullShortUrl.indexOf("/"), fullShortUrl.length()));
                ((HttpServletResponse) response).addCookie(uvCookie);
                uvFirstFlag.set(true);
                //将 uvFirstFlag 设置为 true，表示这是首次访问。
                stringRedisTemplate.opsForSet().add("short-link:stats:uv:" + fullShortUrl, uv.get());
            };
            if(ArrayUtil.isNotEmpty(cookies)){
                Arrays.stream(cookies)
                        .filter(each -> Objects.equals(each.getName(), "uv"))
                        .findFirst()
                        .map(Cookie::getValue)
                        .ifPresentOrElse(each -> {
                            uv.set(each);
                            Long added = stringRedisTemplate.opsForSet().add("short-link:stats:uv:" + fullShortUrl, each);
                            uvFirstFlag.set(added != null && added > 0L);
                        },addResponseCookieTask);
                //非lambda实现
                //if (ArrayUtil.isNotEmpty(cookies)) {
                //    Cookie foundCookie = null;
                //    for (Cookie each : cookies) {
                //        if (Objects.equals(each.getName(), "uv")) {
                //            foundCookie = each;
                //            break;
                //        }
                //    }
                //
                //    if (foundCookie != null) {
                //        String each = foundCookie.getValue();
                //        uv.set(each);
                //        Long added = stringRedisTemplate.opsForSet().add("short-link:stats:uv:" + fullShortUrl, each);
                //        uvFirstFlag.set(added != null && added > 0L);
                //    } else {
                //        addResponseCookieTask.run();
                //    }
                //}
            }else {
                addResponseCookieTask.run();
            }
            //uv,pv,uip访问记录
            String remoteAddr = request.getRemoteAddr();
            Long uipAdded = stringRedisTemplate.opsForSet().add("short-link:stats:uip" + fullShortUrl, remoteAddr);
            boolean uipFirstFlag = uipAdded != null && uipAdded > 0L;
            if(StrUtil.isBlank(gid)){
                LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(ShortLinkGotoDO::getFullShortUrl,fullShortUrl);
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
                gid = shortLinkGotoDO.getGid();
            }
            int hour = DateUtil.hour(new Date(),true);
            Week week = DateUtil.dayOfWeekEnum(new Date());
            int weekValue = week.getIso8601Value();
            LinkAccessStatsDO linkAcessStatsDO = LinkAccessStatsDO.builder()
                    .pv(1)
                    .uv(uvFirstFlag.get() ? 1 : 0)
                    .uip(uipFirstFlag ? 1 : 0)
                    .hour(hour)
                    .weekday(weekValue)
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .build();
            linkAccessStatsMapper.shortLinkStats(linkAcessStatsDO);
            //地区访问记录
            HashMap<String, Object> localeParamMap = new HashMap<>();
            localeParamMap.put("key", statsLocalAmapkey);
            localeParamMap.put("ip", remoteAddr);
            String localeResultStr = HttpUtil.get(ShortLinkConstant.AMAP_REMOTE_URL, localeParamMap);
            JSONObject localeResultObj = JSON.parseObject(localeResultStr);
            String infocode = localeResultObj.getString("infocode");
            String province = localeResultObj.getString("province");
            String city = localeResultObj.getString("city");
            String adcode = localeResultObj.getString("adcode");

            boolean unknowFlag = StrUtil.equals(province,"[]");
            LinkLocaleStatsDO linkLocaleStatsDO = null;
            if (StrUtil.isNotBlank(infocode) && StrUtil.equals(infocode,"10000")){
                linkLocaleStatsDO = LinkLocaleStatsDO.builder()
                        .fullShortUrl(fullShortUrl)
                        .gid(gid)
                        .date(new Date())
                        .cnt(1)
                        .province(unknowFlag ? "未知" : province)
                        .city(unknowFlag ? "未知" : city)
                        .adcode(unknowFlag ? "未知" : adcode)
                        .country("中国")
                        .build();
            }
            linkLocaleStatsMapper.shortLinkLocaleState(linkLocaleStatsDO);
            //操作系统访问记录
            String os = LinkUtil.getOs((HttpServletRequest) request);
            LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .cnt(1)
                    .os(os)
                    .build();
            linkOsStatsMapper.shortLinkOsState(linkOsStatsDO);
            //浏览器访问记录
            String browser = LinkUtil.getBrowser((HttpServletRequest) request);
            LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .cnt(1)
                    .browser(browser)
                    .build();
            linkBrowserStatsMapper.shortLinkBrowserState(linkBrowserStatsDO);

            //设备访问记录
            String device = LinkUtil.getDevice((HttpServletRequest) request);
            LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .cnt(1)
                    .device(device)
                    .build();
            linkDeviceStatsMapper.shortLinkDeviceState(linkDeviceStatsDO);
            //不同网络来源访问记录
            String network = LinkUtil.getNetwork((HttpServletRequest) request);
            LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .cnt(1)
                    .network(network)
                    .build();
            linkNetworkStatsMapper.shortLinkNetworkState(linkNetworkStatsDO);
            //IP访问记录
            LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .user(uv.get())
                    .browser(browser)
                    .os(os)
                    .ip(remoteAddr)
                    .network(network)
                    .device(device)
                    .locale(StrUtil.join("-","中国",province,city))
                    .build();
            linkAccessLogsMapper.insert(linkAccessLogsDO);


        } catch (Throwable ex) {
            System.out.println(ex.getMessage());
            log.error("短链接访问量统计异常");
        }
    }


    private String generateSuffix(ShortLinkCreateReqDTO requestParam) {
        String originUrl = requestParam.getOriginUrl();
        String shortUri;
        int customGenerateCount = 0;
        while (true) {
            if (customGenerateCount > 10) {
                throw new ServiceException("短链接频繁生成，请稍后再试");
            }
            originUrl += UUID.randomUUID().toString();
            //下次调用时，长链接已不同
            shortUri = HashUtil.hashToBase62(originUrl);
            //生成重复短链接后，如果还是用之前的长链接生成，之后的结果都会是相同的
            if (!shortUriCreateCachePenetrationBloomFilter.contains(requestParam.getDomain() + "/" + shortUri)) {
                break;
            }
            customGenerateCount++;

        }
        return shortUri;

    }

    @SneakyThrows
    private String getFavicon(String url){
        //创建URL对象
        URL targetUrl = new URL(url);
        //打开连接
        HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
        // 禁止自动处理重定向
        connection.setInstanceFollowRedirects(false);
        // 设置请求方法为GET
        connection.setRequestMethod("GET");
        //连接
        connection.connect();
        //获取响应码
        int responseCode = connection.getResponseCode();
        // 如果是重定向响应码
        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            //获取重定向的URL
            String redirectUrl = connection.getHeaderField("Location");
            //如果重定向URL不为空
            if (redirectUrl != null) {
                // 创建新的URL对象
                URL newUrl = new URL(redirectUrl);//打开新的连接
                connection = (HttpURLConnection) newUrl.openConnection();//设置请求方法为GET
                connection.setRequestMethod("GET");//连接
                connection.connect();//获取新的响应码
                responseCode = connection.getResponseCode();
            }
        }
        // 如果响应码为200(HTTP_OK)
        if (responseCode == HttpURLConnection.HTTP_OK) {
            Document document = Jsoup.connect(url).get();
            Element faviconLink = document.select("link[rel~=(?i)^(shortcut)?icon]").first();
            if (faviconLink != null) {
                return faviconLink.attr("abs:href");
            }
        }
        return null;
    }
}
