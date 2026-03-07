package xyz.kbws.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author kbws
 * @date 2026/3/7
 * @description:
 */
public class Util {
    private static final char[] BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final long MAX_6_BASE62 = 56800235584L; // 62^6

    /**
     * 将任意字符串转换为固定6位的字母数字混排短码
     */
    public static String shortCode6(String input) {
        try {
            // 1. MD5 哈希
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));

            // 2. 取前 6 个字节拼成 long
            long value = 0;
            for (int i = 0; i < 6; i++) {
                value = (value << 8) | (digest[i] & 0xFFL);
            }

            // 3. 映射到 6 位 base62 空间
            value = value % MAX_6_BASE62;

            // 4. 转成固定 6 位 base62
            char[] result = new char[6];
            for (int i = 5; i >= 0; i--) {
                result[i] = BASE62[(int) (value % 62)];
                value /= 62;
            }

            return new String(result);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

}
