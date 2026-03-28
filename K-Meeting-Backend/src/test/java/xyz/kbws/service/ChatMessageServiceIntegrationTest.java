package xyz.kbws.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.session.SqlSessionFactory;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import xyz.kbws.mapper.ChatMessageMapper;
import xyz.kbws.model.entity.ChatMessage;
import xyz.kbws.model.enums.MessageSendTypeEnum;
import xyz.kbws.model.enums.MessageTypeEnum;
import xyz.kbws.service.impl.ChatMessageServiceImpl;
import xyz.kbws.service.support.MutableClock;
import xyz.kbws.utils.ChatMessageTableUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

@SpringBootTest(classes = ChatMessageServiceIntegrationTest.TestApplication.class)
@ActiveProfiles("test")
class ChatMessageServiceIntegrationTest {

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MutableClock mutableClock;

    @BeforeEach
    void setUp() throws Exception {
        cleanupTables();
        setCurrentTime(2026, 3, 28, 10, 0, 0);
    }

    @AfterEach
    void tearDown() throws Exception {
        cleanupTables();
    }

    @Test
    void shouldKeepCurrentDayMessagesInHotTable() throws Exception {
        ChatMessage chatMessage = buildMessage(1001L, 101, nowMillis(), "today-message");

        boolean saved = chatMessageService.save(chatMessage);

        Assertions.assertTrue(saved);
        Assertions.assertEquals(1, countRows("chatMessage"));
        Assertions.assertEquals(0, countPartitionTables());
        Assertions.assertNotNull(chatMessageService.getById(chatMessage.getId()));
        Assertions.assertEquals(1, chatMessageService.listHistoryByMeetingId(101, null, null, null, 10).size());
    }

    @Test
    void shouldArchiveYesterdayMessagesWhenCrossingMidnight() throws Exception {
        setCurrentTime(2026, 3, 27, 23, 50, 0);
        ChatMessage yesterdayMessage = buildMessage(2001L, 202, nowMillis(), "before-midnight");
        chatMessageService.save(yesterdayMessage);
        Assertions.assertEquals(1, countRows("chatMessage"));

        setCurrentTime(2026, 3, 28, 0, 10, 0);
        ChatMessage todayMessage = buildMessage(2002L, 202, nowMillis(), "after-midnight");
        chatMessageService.save(todayMessage);

        Assertions.assertEquals(1, countRows("chatMessage"));
        Assertions.assertEquals(1, countRows("chatMessage_2026_03_27"));

        List<ChatMessage> history = chatMessageService.listHistoryByMeetingId(202, null, null, null, 10);
        Assertions.assertEquals(2, history.size());
        Assertions.assertEquals("after-midnight", history.get(0).getContent());
        Assertions.assertEquals("before-midnight", history.get(1).getContent());
    }

    @Test
    void shouldSupportHistoryAggregationAndPaginationForCrossDayMeeting() throws Exception {
        setCurrentTime(2026, 3, 27, 23, 58, 0);
        ChatMessage dayOneLast = buildMessage(3001L, 303, nowMillis(), "day-one-last");
        chatMessageService.save(dayOneLast);

        setCurrentTime(2026, 3, 28, 0, 1, 0);
        ChatMessage dayTwoFirst = buildMessage(3002L, 303, nowMillis(), "day-two-first");
        chatMessageService.save(dayTwoFirst);

        setCurrentTime(2026, 3, 28, 8, 0, 0);
        ChatMessage dayTwoSecond = buildMessage(3003L, 303, nowMillis(), "day-two-second");
        chatMessageService.save(dayTwoSecond);

        List<ChatMessage> latestTwo = chatMessageService.listHistoryByMeetingId(303, null, null, null, 2);
        Assertions.assertEquals(2, latestTwo.size());
        Assertions.assertEquals("day-two-second", latestTwo.get(0).getContent());
        Assertions.assertEquals("day-two-first", latestTwo.get(1).getContent());

        List<ChatMessage> olderMessages = chatMessageService.listHistoryByMeetingId(303, null, null, dayTwoSecond.getSendTime(), 10);
        Assertions.assertEquals(2, olderMessages.size());
        Assertions.assertEquals("day-two-first", olderMessages.get(0).getContent());
        Assertions.assertEquals("day-one-last", olderMessages.get(1).getContent());
    }

    @Test
    void shouldArchiveOnlyOnceWhenCalledRepeatedly() throws Exception {
        setCurrentTime(2026, 3, 27, 21, 0, 0);
        ChatMessage expiredMessage = buildMessage(4001L, 404, nowMillis(), "archive-once");
        chatMessageService.save(expiredMessage);

        setCurrentTime(2026, 3, 28, 9, 0, 0);
        int firstArchivedCount = chatMessageService.archiveExpiredMessages();
        int secondArchivedCount = chatMessageService.archiveExpiredMessages();

        Assertions.assertEquals(1, firstArchivedCount);
        Assertions.assertEquals(0, secondArchivedCount);
        Assertions.assertEquals(0, countRows("chatMessage"));
        Assertions.assertEquals(1, countRows("chatMessage_2026_03_27"));
    }

    @Test
    void shouldWriteDelayedHistoricalMessageDirectlyToArchiveTable() throws Exception {
        long historicalSendTime = ZonedDateTime.of(2026, 3, 26, 20, 0, 0, 0, ChatMessageTableUtil.TABLE_ZONE_ID)
                .toInstant()
                .toEpochMilli();
        ChatMessage delayedMessage = buildMessage(5001L, 505, historicalSendTime, "delayed-history");

        chatMessageService.save(delayedMessage);

        Assertions.assertEquals(0, countRows("chatMessage"));
        Assertions.assertEquals(1, countRows("chatMessage_2026_03_26"));
        ChatMessage loaded = chatMessageService.getById(delayedMessage.getId());
        Assertions.assertNotNull(loaded);
        Assertions.assertEquals("delayed-history", loaded.getContent());
    }

    private ChatMessage buildMessage(Long id, Integer meetingId, Long sendTime, String content) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(id);
        chatMessage.setMeetingId(meetingId);
        chatMessage.setType(MessageTypeEnum.CHAT_TEXT_MESSAGE.getValue());
        chatMessage.setContent(content);
        chatMessage.setSendUserId(1);
        chatMessage.setSendUserNickName("tester");
        chatMessage.setSendTime(sendTime);
        chatMessage.setReceiveType(MessageSendTypeEnum.GROUP.getType());
        chatMessage.setStatus(1);
        return chatMessage;
    }

    private void setCurrentTime(int year, int month, int day, int hour, int minute, int second) {
        Instant instant = ZonedDateTime.of(year, month, day, hour, minute, second, 0, ChatMessageTableUtil.TABLE_ZONE_ID)
                .toInstant();
        mutableClock.setInstant(instant);
    }

    private long nowMillis() {
        return mutableClock.millis();
    }

    private int countPartitionTables() throws Exception {
        int count = 0;
        try (Connection connection = dataSource.getConnection()) {
            ResultSet resultSet = connection.getMetaData().getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"});
            while (resultSet.next()) {
                String tableName = resultSet.getString("TABLE_NAME");
                if (tableName != null && tableName.matches("^chatMessage_\\d{4}_\\d{2}_\\d{2}$")) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countRows(String tableName) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(1) FROM " + tableName)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private void cleanupTables() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            List<String> partitionTables = new java.util.ArrayList<>();
            ResultSet resultSet = connection.getMetaData().getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"});
            while (resultSet.next()) {
                String tableName = resultSet.getString("TABLE_NAME");
                if (tableName != null && tableName.matches("^chatMessage_\\d{4}_\\d{2}_\\d{2}$")) {
                    partitionTables.add(tableName);
                }
            }
            for (String partitionTable : partitionTables) {
                statement.execute("DROP TABLE IF EXISTS " + partitionTable);
            }
            statement.execute("DELETE FROM chatMessage");
        }
    }

    @EnableAutoConfiguration
    @EnableTransactionManagement
    @Import({
            ChatMessageServiceImpl.class,
            ChatMessagePartitionManager.class,
            MybatisPlusTestConfiguration.class,
            ClockTestConfiguration.class
    })
    static class TestApplication {

        @Bean
        public MapperFactoryBean<ChatMessageMapper> chatMessageMapper(SqlSessionFactory sqlSessionFactory) {
            MapperFactoryBean<ChatMessageMapper> factoryBean = new MapperFactoryBean<>(ChatMessageMapper.class);
            factoryBean.setSqlSessionFactory(sqlSessionFactory);
            return factoryBean;
        }
    }

    @TestConfiguration
    static class ClockTestConfiguration {

        @Bean(name = "chatMessageClock")
        public MutableClock chatMessageClock() {
            return new MutableClock(
                    ZonedDateTime.of(2026, 3, 28, 10, 0, 0, 0, ChatMessageTableUtil.TABLE_ZONE_ID).toInstant(),
                    ChatMessageTableUtil.TABLE_ZONE_ID
            );
        }
    }

    @TestConfiguration
    static class MybatisPlusTestConfiguration {

        @Bean
        public MybatisPlusInterceptor mybatisPlusInterceptor() {
            MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
            interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
            return interceptor;
        }
    }
}
