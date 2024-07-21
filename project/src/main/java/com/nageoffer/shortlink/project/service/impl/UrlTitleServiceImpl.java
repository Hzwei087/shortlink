package com.nageoffer.shortlink.project.service.impl;

import com.nageoffer.shortlink.project.service.UrlTitleService;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class UrlTitleServiceImpl implements UrlTitleService {
    @SneakyThrows
    @Override
    public String getTitleByUrl(String url)  {
        URL tagetUrl = new URL(url);
        //转换 URL: 将传入的 URL 字符串转换为 URL 对象。
        HttpURLConnection connection = (HttpURLConnection) tagetUrl.openConnection();
        connection.setRequestMethod("GET");
        //打开连接: 打开与目标 URL 的连接，并设置请求方法为 "GET"。
        connection.connect();
        //建立连接: 与目标 URL 建立连接。

        int responseCode = connection.getResponseCode();
        //获取响应代码: 获取服务器返回的响应代码。
        if (responseCode == HttpURLConnection.HTTP_OK) {
            Document document = Jsoup.connect(url).get();
            return document.title();
            //检查响应代码: 如果响应代码是 200，使用 Jsoup 库连接到目标 URL 并解析 HTML 文档，返回文档的标题。
        }






        return "Erro while fetching title";

    }
}
