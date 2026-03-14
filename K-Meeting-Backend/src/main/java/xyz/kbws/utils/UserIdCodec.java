package xyz.kbws.utils;

import cn.hutool.core.util.StrUtil;

import java.math.BigInteger;

/**
 * 将正整数 userId 映射为短 Base62 字符串，并支持无碰撞反解。
 * 实现对外 ID 混淆。
 */
public final class UserIdCodec {

    private static final char[] BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int ENCODED_LENGTH = 6;
    private static final long MOD = 1L << 32;
    private static final long MASK_32 = MOD - 1;
    private static final long MULTIPLIER = 2654435761L;
    private static final long OFFSET = 1013904223L;
    private static final long MULTIPLIER_INVERSE =
            BigInteger.valueOf(MULTIPLIER).modInverse(BigInteger.ONE.shiftLeft(32)).longValue();

    private UserIdCodec() {
    }

    public static String encode(Integer userId) {
        if (userId == null || userId < 0) {
            throw new IllegalArgumentException("userId must be a non-negative integer");
        }
        long value = Integer.toUnsignedLong(userId);
        long mixed = (MULTIPLIER * value + OFFSET) & MASK_32;
        return leftPad(toBase62(mixed));
    }

    public static Integer decode(String encodedUserId) {
        if (encodedUserId == null || StrUtil.isBlank(encodedUserId)) {
            throw new IllegalArgumentException("encodedUserId must not be blank");
        }
        long mixed = fromBase62(encodedUserId);
        if (mixed > MASK_32) {
            throw new IllegalArgumentException("encodedUserId is out of range");
        }
        long normalized = (mixed - OFFSET) & MASK_32;
        long original = (MULTIPLIER_INVERSE * normalized) & MASK_32;
        if (original > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("decoded userId exceeds signed int range");
        }
        return (int) original;
    }

    private static String toBase62(long value) {
        if (value == 0) {
            return "0";
        }
        StringBuilder sb = new StringBuilder();
        long current = value;
        while (current > 0) {
            int index = (int) (current % BASE62.length);
            sb.append(BASE62[index]);
            current /= BASE62.length;
        }
        return sb.reverse().toString();
    }

    private static long fromBase62(String value) {
        long result = 0;
        for (int i = 0; i < value.length(); i++) {
            int digit = indexOf(value.charAt(i));
            if (digit < 0) {
                throw new IllegalArgumentException("encodedUserId contains invalid base62 characters");
            }
            result = result * BASE62.length + digit;
        }
        return result;
    }

    private static int indexOf(char ch) {
        for (int i = 0; i < BASE62.length; i++) {
            if (BASE62[i] == ch) {
                return i;
            }
        }
        return -1;
    }

    private static String leftPad(String value) {
        if (value.length() >= ENCODED_LENGTH) {
            return value;
        }
        StringBuilder sb = new StringBuilder(ENCODED_LENGTH);
        for (int i = value.length(); i < ENCODED_LENGTH; i++) {
            sb.append(BASE62[0]);
        }
        sb.append(value);
        return sb.toString();
    }
}
