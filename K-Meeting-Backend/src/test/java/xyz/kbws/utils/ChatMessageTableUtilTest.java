package xyz.kbws.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

class ChatMessageTableUtilTest {

    @Test
    void shouldResolveArchiveTableNameByShanghaiDate() {
        long sendTime = ZonedDateTime.of(2026, 3, 28, 0, 30, 0, 0, ChatMessageTableUtil.TABLE_ZONE_ID)
                .toInstant()
                .toEpochMilli();

        String tableName = ChatMessageTableUtil.resolveTableName(sendTime);

        Assertions.assertEquals("chatMessage_2026_03_28", tableName);
    }

    @Test
    void shouldDetectCurrentDayCorrectlyAcrossMidnight() {
        long currentTime = ZonedDateTime.of(2026, 3, 28, 0, 10, 0, 0, ChatMessageTableUtil.TABLE_ZONE_ID)
                .toInstant()
                .toEpochMilli();
        long yesterdayTime = ZonedDateTime.of(2026, 3, 27, 23, 59, 59, 0, ChatMessageTableUtil.TABLE_ZONE_ID)
                .toInstant()
                .toEpochMilli();
        long todayTime = ZonedDateTime.of(2026, 3, 28, 0, 0, 1, 0, ChatMessageTableUtil.TABLE_ZONE_ID)
                .toInstant()
                .toEpochMilli();

        Assertions.assertFalse(ChatMessageTableUtil.isSameDay(yesterdayTime, currentTime));
        Assertions.assertTrue(ChatMessageTableUtil.isSameDay(todayTime, currentTime));
    }
}
