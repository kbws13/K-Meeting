package xyz.kbws.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import xyz.kbws.model.entity.ChatMessage;

import java.util.List;

/**
 * @author housenyao
 * @description 针对表【chatMessage(聊天消息表)】的数据库操作Mapper
 * @createDate 2026-03-28 18:54:48
 * @Entity xyz.kbws.model.entity.chatMessage
 */
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    int insertPartition(@Param("tableName") String tableName, @Param("entity") ChatMessage entity);

    int batchInsertPartition(@Param("tableName") String tableName, @Param("entities") List<ChatMessage> entities);

    int updatePartitionById(@Param("tableName") String tableName, @Param("entity") ChatMessage entity);

    int deletePartitionById(@Param("tableName") String tableName, @Param("id") Long id);

    List<ChatMessage> listPrivateHistoryByPageAcrossTables(@Param("tableNames") List<String> tableNames,
                                                           @Param("meetingId") Integer meetingId,
                                                           @Param("currentUserId") Integer currentUserId,
                                                           @Param("targetUserId") Integer targetUserId,
                                                           @Param("receiveType") Integer receiveType,
                                                           @Param("maxMessageId") Long maxMessageId,
                                                           @Param("offset") Long offset,
                                                           @Param("limit") Long limit);

    Long countPrivateHistoryAcrossTables(@Param("tableNames") List<String> tableNames,
                                         @Param("meetingId") Integer meetingId,
                                         @Param("currentUserId") Integer currentUserId,
                                         @Param("targetUserId") Integer targetUserId,
                                         @Param("receiveType") Integer receiveType,
                                         @Param("maxMessageId") Long maxMessageId);

    List<ChatMessage> listByMeetingIdAcrossTables(@Param("tableNames") List<String> tableNames,
                                                  @Param("meetingId") Integer meetingId,
                                                  @Param("startSendTime") Long startSendTime,
                                                  @Param("endSendTime") Long endSendTime,
                                                  @Param("beforeSendTime") Long beforeSendTime,
                                                  @Param("limit") Integer limit);

    ChatMessage getByIdAcrossTables(@Param("tableNames") List<String> tableNames, @Param("id") Long id);

    List<ChatMessage> listExpiredMessagesFromHotTable(@Param("cutoffSendTime") Long cutoffSendTime);

    Long countExpiredMessagesInHotTable(@Param("cutoffSendTime") Long cutoffSendTime);

    int deleteHotMessagesByIds(@Param("ids") List<Long> ids);
}

