package xyz.kbws.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.kbws.common.ErrorCode;
import xyz.kbws.exception.BusinessException;
import xyz.kbws.mapper.ChatMessageMapper;
import xyz.kbws.model.entity.ChatMessage;
import xyz.kbws.service.ChatMessagePartitionManager;
import xyz.kbws.service.ChatMessageService;
import xyz.kbws.utils.ChatMessageTableUtil;

import javax.annotation.Resource;
import java.io.Serializable;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author housenyao
 * @description 针对表【chatMessage(聊天消息表)】的数据库操作Service实现
 * @createDate 2026-03-28 18:54:48
 */
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
        implements ChatMessageService {

    private static final int DEFAULT_HISTORY_LIMIT = 50;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    @Resource
    private ChatMessagePartitionManager chatMessagePartitionManager;

    @Resource(name = "chatMessageClock")
    private Clock clock;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(ChatMessage entity) {
        return savePartitioned(entity);
    }

    @Override
    public ChatMessage getById(Serializable id) {
        if (id == null) {
            return null;
        }
        if (id instanceof Number) {
            return getByIdAcrossPartitions(((Number) id).longValue());
        }
        try {
            return getByIdAcrossPartitions(Long.valueOf(String.valueOf(id)));
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息 ID 非法");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean savePartitioned(ChatMessage chatMessage) {
        validateChatMessage(chatMessage);
        if (chatMessage.getId() == null) {
            chatMessage.setId(IdUtil.getSnowflakeNextId());
        }
        normalizeSendTime(chatMessage);
        archiveExpiredMessagesIfNeeded();
        if (ChatMessageTableUtil.isSameDay(chatMessage.getSendTime(), currentTimeMillis())) {
            return super.save(chatMessage);
        }
        String tableName = ChatMessageTableUtil.resolveTableName(chatMessage.getSendTime());
        chatMessagePartitionManager.ensurePartitionTable(tableName);
        return chatMessageMapper.insertPartition(tableName, chatMessage) > 0;
    }

    @Override
    public List<ChatMessage> listHistoryByMeetingId(Integer meetingId,
                                                    Long startSendTime,
                                                    Long endSendTime,
                                                    Long beforeSendTime,
                                                    Integer limit) {
        if (meetingId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "meetingId 不能为空");
        }
        if (startSendTime != null && endSendTime != null && startSendTime > endSendTime) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "开始时间不能大于结束时间");
        }
        archiveExpiredMessagesIfNeeded();
        int queryLimit = limit == null || limit <= 0 ? DEFAULT_HISTORY_LIMIT : limit;
        List<ChatMessage> mergedMessages = new ArrayList<>();
        List<ChatMessage> hotMessages = listHotTableMessages(meetingId, startSendTime, endSendTime, beforeSendTime, queryLimit);
        mergedMessages.addAll(hotMessages);
        List<String> tableNames = resolveArchiveTables(startSendTime, endSendTime);
        if (!tableNames.isEmpty()) {
            mergedMessages.addAll(chatMessageMapper.listByMeetingIdAcrossTables(
                    tableNames,
                    meetingId,
                    startSendTime,
                    endSendTime,
                    beforeSendTime,
                    queryLimit
            ));
        }
        if (mergedMessages.isEmpty()) {
            return Collections.emptyList();
        }
        Comparator<ChatMessage> comparator = Comparator
                .comparing(ChatMessage::getSendTime, Comparator.nullsLast(Long::compareTo))
                .thenComparing(ChatMessage::getId, Comparator.nullsLast(Long::compareTo));
        return mergedMessages.stream()
                .sorted(comparator.reversed())
                .limit(queryLimit)
                .collect(Collectors.toList());
    }

    @Override
    public ChatMessage getByIdAcrossPartitions(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息 ID 不能为空");
        }
        archiveExpiredMessagesIfNeeded();
        ChatMessage hotMessage = baseMapper.selectById(id);
        if (hotMessage != null) {
            return hotMessage;
        }
        List<String> tableNames = resolveArchiveTables(null, null);
        if (tableNames.isEmpty()) {
            return null;
        }
        return chatMessageMapper.getByIdAcrossTables(tableNames, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int archiveExpiredMessages() {
        long cutoffSendTime = ChatMessageTableUtil.startOfTodayMillis(currentTimeMillis());
        List<ChatMessage> expiredMessages = chatMessageMapper.listExpiredMessagesFromHotTable(cutoffSendTime);
        if (expiredMessages == null || expiredMessages.isEmpty()) {
            return 0;
        }
        Map<String, List<ChatMessage>> groupedMessages = new LinkedHashMap<>();
        for (ChatMessage expiredMessage : expiredMessages) {
            String tableName = ChatMessageTableUtil.resolveTableName(expiredMessage.getSendTime());
            groupedMessages.computeIfAbsent(tableName, key -> new ArrayList<>()).add(expiredMessage);
        }
        for (Map.Entry<String, List<ChatMessage>> entry : groupedMessages.entrySet()) {
            chatMessagePartitionManager.ensurePartitionTable(entry.getKey());
            chatMessageMapper.batchInsertPartition(entry.getKey(), entry.getValue());
        }
        List<Long> ids = expiredMessages.stream()
                .map(ChatMessage::getId)
                .collect(Collectors.toList());
        if (!ids.isEmpty()) {
            chatMessageMapper.deleteHotMessagesByIds(ids);
        }
        return expiredMessages.size();
    }

    private void archiveExpiredMessagesIfNeeded() {
        long cutoffSendTime = ChatMessageTableUtil.startOfTodayMillis(currentTimeMillis());
        Long expiredCount = chatMessageMapper.countExpiredMessagesInHotTable(cutoffSendTime);
        if (expiredCount != null && expiredCount > 0) {
            archiveExpiredMessages();
        }
    }

    private List<String> resolveArchiveTables(Long startSendTime, Long endSendTime) {
        List<String> tableNames = chatMessagePartitionManager.listPartitionTables();
        if (tableNames == null || tableNames.isEmpty()) {
            return Collections.emptyList();
        }
        return tableNames.stream()
                .filter(ChatMessageTableUtil::isValidTableName)
                .filter(tableName -> ChatMessageTableUtil.matchesRange(tableName, startSendTime, endSendTime))
                .collect(Collectors.toList());
    }

    private List<ChatMessage> listHotTableMessages(Integer meetingId,
                                                   Long startSendTime,
                                                   Long endSendTime,
                                                   Long beforeSendTime,
                                                   int limit) {
        QueryWrapper<ChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("meetingId", meetingId);
        if (startSendTime != null) {
            queryWrapper.ge("sendTime", startSendTime);
        }
        if (endSendTime != null) {
            queryWrapper.le("sendTime", endSendTime);
        }
        if (beforeSendTime != null) {
            queryWrapper.lt("sendTime", beforeSendTime);
        }
        queryWrapper.orderByDesc("sendTime", "id");
        queryWrapper.last("limit " + limit);
        return baseMapper.selectList(queryWrapper);
    }

    private void normalizeSendTime(ChatMessage chatMessage) {
        if (chatMessage.getSendTime() == null) {
            chatMessage.setSendTime(currentTimeMillis());
        }
    }

    private long currentTimeMillis() {
        return clock.millis();
    }

    private void validateChatMessage(ChatMessage chatMessage) {
        if (chatMessage == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "聊天消息不能为空");
        }
        if (chatMessage.getMeetingId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "meetingId 不能为空");
        }
        if (chatMessage.getType() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        }
        if (chatMessage.getSendUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "发送人不能为空");
        }
        if (chatMessage.getReceiveType() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接收类型不能为空");
        }
    }
}

