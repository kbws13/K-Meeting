package xyz.kbws.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * chatMessage 按天分表工具。
 */
public final class ChatMessageTableUtil {

    public static final String BASE_TABLE_NAME = "chat_message";
    public static final ZoneId TABLE_ZONE_ID = ZoneId.of("Asia/Shanghai");

    private static final String TABLE_PREFIX = BASE_TABLE_NAME + "_";
    private static final DateTimeFormatter TABLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM_dd");
    private static final Pattern PARTITION_TABLE_PATTERN = Pattern.compile("^chatMessage_\\d{4}_\\d{2}_\\d{2}$");

    private ChatMessageTableUtil() {
    }

    public static String resolveTableName(Long sendTime) {
        if (sendTime == null || sendTime <= 0) {
            throw new IllegalArgumentException("sendTime 非法，无法计算 chatMessage 分表名");
        }
        LocalDate localDate = Instant.ofEpochMilli(sendTime).atZone(TABLE_ZONE_ID).toLocalDate();
        return TABLE_PREFIX + localDate.format(TABLE_DATE_FORMATTER);
    }

    public static long startOfTodayMillis(long currentTimeMillis) {
        LocalDate localDate = Instant.ofEpochMilli(currentTimeMillis).atZone(TABLE_ZONE_ID).toLocalDate();
        return localDate.atStartOfDay(TABLE_ZONE_ID).toInstant().toEpochMilli();
    }

    public static boolean isSameDay(Long sendTime, long currentTimeMillis) {
        if (sendTime == null || sendTime <= 0) {
            return false;
        }
        LocalDate sendDate = Instant.ofEpochMilli(sendTime).atZone(TABLE_ZONE_ID).toLocalDate();
        LocalDate currentDate = Instant.ofEpochMilli(currentTimeMillis).atZone(TABLE_ZONE_ID).toLocalDate();
        return sendDate.equals(currentDate);
    }

    public static boolean isValidTableName(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return false;
        }
        return BASE_TABLE_NAME.equals(tableName) || PARTITION_TABLE_PATTERN.matcher(tableName).matches();
    }

    public static void validateTableName(String tableName) {
        if (!isValidTableName(tableName)) {
            throw new IllegalArgumentException("非法的 chatMessage 表名: " + tableName);
        }
    }

    public static boolean matchesRange(String tableName, Long startSendTime, Long endSendTime) {
        validateTableName(tableName);
        if (BASE_TABLE_NAME.equals(tableName)) {
            return true;
        }
        LocalDate tableDate = parseTableDate(tableName);
        if (tableDate == null) {
            return false;
        }
        if (startSendTime != null) {
            LocalDate startDate = Instant.ofEpochMilli(startSendTime).atZone(TABLE_ZONE_ID).toLocalDate();
            if (tableDate.isBefore(startDate)) {
                return false;
            }
        }
        if (endSendTime != null) {
            LocalDate endDate = Instant.ofEpochMilli(endSendTime).atZone(TABLE_ZONE_ID).toLocalDate();
            if (tableDate.isAfter(endDate)) {
                return false;
            }
        }
        return true;
    }

    private static LocalDate parseTableDate(String tableName) {
        if (!PARTITION_TABLE_PATTERN.matcher(tableName).matches()) {
            return null;
        }
        String dateText = tableName.substring(TABLE_PREFIX.length());
        try {
            return LocalDate.parse(dateText, TABLE_DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
