package com.nageoffer.shortlink.admin.utils;

import java.security.SecureRandom;
/**
 * 分组ID随机生成器
 */
public final class RandomStringUtil {

    // 字符集，包括数字和英文字母
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();


    public static String generateRandom(){
        return generateRandomString(6);
    }

    /**
     * 生成指定长度的随机字符串
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String generateRandomString(int length) {
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            result.append(CHARACTERS.charAt(index));
        }
        return result.toString();
    }

}
