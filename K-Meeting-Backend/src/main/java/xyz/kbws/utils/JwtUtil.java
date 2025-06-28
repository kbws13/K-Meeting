package xyz.kbws.utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
public class JwtUtil {
    private static final String ASCII_UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom random = new SecureRandom();

    public static String createToken(Integer userId) {
        String hashValue = md5Hash(String.valueOf(userId));
        return String.format("%s:%s:%s", generateRandomPrefix(), hashValue, generateRandomSuffix());
    }

    private static String generateRandomPart(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(JwtUtil.ASCII_UPPERCASE.length());
            sb.append(JwtUtil.ASCII_UPPERCASE.charAt(randomIndex));
        }
        return sb.toString();
    }

    private static String generateRandomPrefix() {
        return generateRandomPart(1);
    }

    private static String generateRandomSuffix() {
        return generateRandomPart(2);
    }

    private static String md5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            BigInteger no = new BigInteger(1, hashBytes);
            StringBuilder hexString = new StringBuilder(no.toString(16));
            while (hexString.length() < 32) {
                hexString.insert(0, '0');
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

}
