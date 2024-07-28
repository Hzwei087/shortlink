/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nageoffer.shortlink.admin.utils;

import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 封装 EasyExcel 操作 Web 工具方法
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
public class EasyExcelWebUtil {

    /**
     * 向浏览器写入 Excel 响应，直接返回用户下载数据
     *
     * @param response 响应
     * @param fileName 文件名
     * @param clazz    指定写入类
     * @param data     写入数据
     */
    @SneakyThrows
    public static void write(HttpServletResponse response, String fileName, Class<?> clazz, List<?> data) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        //设置响应内容类型和字符编码
        //setContentType：设置响应的内容类型为Excel文件格式。
        //setCharacterEncoding：设置响应的字符编码为UTF-8。

        fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        //处理文件名编码，URLEncoder.encode：使用UTF-8编码对文件名进行URL编码，确保文件名中包含的特殊字符在传输过程中不会出现问题。
        //replaceAll("\\+", "%20")：将编码后的加号（+）替换为百分号编码的空格（%20），因为在URL编码中空格会被编码为+，但在文件名中应显示为空格。

        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), clazz).sheet("Sheet").doWrite(data);
        //response.getOutputStream()：获取响应的输出流，将Excel文件内容写入该输出流。
        //clazz：指定Excel文件的数据结构。
        //sheet("Sheet")：创建一个名为"Sheet"的工作表。
        //doWrite(data)：将数据写入Excel文件。

    }
}
