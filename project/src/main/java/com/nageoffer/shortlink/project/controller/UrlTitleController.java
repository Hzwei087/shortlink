package com.nageoffer.shortlink.project.controller;

import com.nageoffer.shortlink.project.common.convention.result.Result;
import com.nageoffer.shortlink.project.common.convention.result.Results;
import com.nageoffer.shortlink.project.service.UrlTitleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UrlTitleController {
    /**
     * @RequiredArgsConstructor 是 Lombok 库中的一个注解，它会自动生成一个包含所有 final 字段的构造函数。
     * 由于 UrlTitleService 被声明为 final，Lombok 会为 UrlTitleController 生成一个包含 UrlTitleService 参数的构造函数，
     * 这个构造函数将被用于依赖注入。
     */

    private final UrlTitleService urlTitleService;
    /**
     * 根据URL获取对应网站的标题
     * @param  url
     * @return
     */

    @GetMapping("/api/short-link/v1/title")
    public Result<String> getTitleByUrl(@RequestParam("url") String url){
        String title = urlTitleService.getTitleByUrl(url);
        return Results.success(title);
    }
}
