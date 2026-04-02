package xyz.kbws.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import xyz.kbws.common.ErrorCode;
import xyz.kbws.config.AppConfig;
import xyz.kbws.constant.FileConstant;
import xyz.kbws.exception.BusinessException;
import xyz.kbws.ffmpeg.FFmpegComponent;
import xyz.kbws.mapper.ChatMessageMapper;
import xyz.kbws.model.dto.message.MessageSendDto;
import xyz.kbws.model.entity.ChatMessage;
import xyz.kbws.model.enums.*;
import xyz.kbws.model.query.ChatMessageQuery;
import xyz.kbws.service.ChatMessagePartitionManager;
import xyz.kbws.service.ChatMessageService;
import xyz.kbws.utils.ChatMessageTableUtil;
import xyz.kbws.websocket.message.MessageHandler;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.Clock;
import java.util.*;
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

    @Resource
    private MessageHandler messageHandler;

    @Resource(name = "chatMessageClock")
    private Clock clock;

    @Resource
    private AppConfig appConfig;

    @Resource
    private FFmpegComponent fFmpegComponent;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(ChatMessage entity) {
        return savePartitioned(entity);
    }

    @Override
    public Page<ChatMessage> findPrivateHistoryByPage(ChatMessageQuery chatMessageQuery) {
        validatePrivateHistoryQuery(chatMessageQuery);
        archiveExpiredMessagesIfNeeded();

        Page<ChatMessage> page = new Page<>(chatMessageQuery.getCurrent(), chatMessageQuery.getPageSize());
        List<String> tableNames = new ArrayList<>();
        tableNames.add(ChatMessageTableUtil.BASE_TABLE_NAME);
        tableNames.addAll(resolveArchiveTables(null, null));

        Long total = chatMessageMapper.countPrivateHistoryAcrossTables(
                tableNames,
                chatMessageQuery.getMeetingId(),
                chatMessageQuery.getCurrentUserId(),
                chatMessageQuery.getUserId(),
                MessageSendTypeEnum.USER.getType(),
                chatMessageQuery.getMaxMessageId()
        );
        page.setTotal(total == null ? 0L : total);
        if (page.getTotal() == 0) {
            page.setRecords(Collections.emptyList());
            return page;
        }

        long offset = chatMessageQuery.getMaxMessageId() == null
                ? Math.max(chatMessageQuery.getCurrent() - 1, 0) * chatMessageQuery.getPageSize()
                : 0L;
        List<ChatMessage> records = chatMessageMapper.listPrivateHistoryByPageAcrossTables(
                tableNames,
                chatMessageQuery.getMeetingId(),
                chatMessageQuery.getCurrentUserId(),
                chatMessageQuery.getUserId(),
                MessageSendTypeEnum.USER.getType(),
                chatMessageQuery.getMaxMessageId(),
                offset,
                chatMessageQuery.getPageSize()
        );
        page.setRecords(records);
        return page;
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
    public boolean updateById(ChatMessage entity) {
        if (entity == null || entity.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息 ID 不能为空");
        }
        archiveExpiredMessagesIfNeeded();
        ChatMessage storedMessage = getByIdAcrossPartitions(entity.getId());
        if (storedMessage == null) {
            return false;
        }

        ChatMessage mergedMessage = BeanUtil.copyProperties(storedMessage, ChatMessage.class);
        BeanUtil.copyProperties(entity, mergedMessage, CopyOptions.create().ignoreNullValue());
        mergedMessage.setId(storedMessage.getId());
        validateChatMessage(mergedMessage);

        String sourceTableName = resolveStorageTableName(storedMessage.getSendTime());
        String targetTableName = resolveStorageTableName(mergedMessage.getSendTime());
        if (sourceTableName.equals(targetTableName)) {
            return updateStoredMessage(targetTableName, mergedMessage);
        }

        insertStoredMessage(targetTableName, mergedMessage);
        if (!deleteStoredMessage(sourceTableName, storedMessage.getId())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除原聊天消息失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) {
        Long messageId = parseMessageId(id);
        if (messageId == null) {
            return false;
        }
        archiveExpiredMessagesIfNeeded();
        ChatMessage storedMessage = getByIdAcrossPartitions(messageId);
        if (storedMessage == null) {
            return false;
        }
        return deleteStoredMessage(resolveStorageTableName(storedMessage.getSendTime()), messageId);
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

    @Override
    public void saveChatMessage(ChatMessage chatMessage) {
        if (!ArrayUtil.contains(new Integer[]{MessageTypeEnum.CHAT_TEXT_MESSAGE.getValue(), MessageTypeEnum.CHAT_MEDIA_MESSAGE.getValue()}, chatMessage.getType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ReceiveTypeEnum receiveTypeEnum = ReceiveTypeEnum.getByValue(chatMessage.getReceiveType());
        if (receiveTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (receiveTypeEnum == ReceiveTypeEnum.USER && chatMessage.getReceiveUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByValue(chatMessage.getType());
        if (messageTypeEnum == MessageTypeEnum.CHAT_TEXT_MESSAGE) {
            if (StrUtil.isEmpty(chatMessage.getContent())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
            chatMessage.setStatus(MessageStatusEnum.SENT.getValue());
        } else if (messageTypeEnum == MessageTypeEnum.CHAT_MEDIA_MESSAGE) {
            if (StrUtil.isEmpty(chatMessage.getFileName()) || chatMessage.getFileSize() == null || chatMessage.getFileType() == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
            chatMessage.setFileSuffix(FileUtil.getSuffix(chatMessage.getFileName()));
            chatMessage.setStatus(MessageStatusEnum.SENDING.getValue());
        }
        chatMessage.setId(IdUtil.getSnowflakeNextId());
        if (!savePartitioned(chatMessage)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存聊天消息失败");
        }
        MessageSendDto messageSendDto = BeanUtil.copyProperties(chatMessage, MessageSendDto.class);
        if (ReceiveTypeEnum.USER == receiveTypeEnum) {
            messageSendDto.setMessageSend2Type(MessageSendTypeEnum.USER.getType());
            messageHandler.sendMessage(messageSendDto);
            // 给自己发一条
            messageSendDto.setReceiveUserId(chatMessage.getSendUserId());
            messageHandler.sendMessage(messageSendDto);
        } else {
            messageSendDto.setMessageSend2Type(MessageSendTypeEnum.GROUP.getType());
            messageHandler.sendMessage(messageSendDto);
        }
    }

    @Override
    public void uploadFile(MultipartFile file, Integer meetingId, Long messageId, Long sendTime) throws IOException {
        String month = DateUtil.format(new Date(), "yyyyMM");
        String folder = appConfig.getProjectFolder() + FileConstant.FILE_FOLDER_FILE + month;
        File folderFile = new File(folder);
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }
        String filePath = folder + "/" + messageId;

        String fileName = file.getOriginalFilename();
        String fileSuffix = FileNameUtil.getSuffix(fileName);
        FileTypeEnum fileTypeEnum = FileTypeEnum.getBySuffix(fileSuffix);
        if (fileTypeEnum == FileTypeEnum.IMAGE) {
            File tempFile = new File(appConfig.getProjectFolder() + FileConstant.FILE_FOLDER_TEMP + RandomUtil.randomString(30));
            file.transferTo(tempFile);
            filePath = filePath + FileConstant.IMAGE_SUFFIX;
            filePath = fFmpegComponent.transferImageType(tempFile, filePath);
            fFmpegComponent.createImageThumbnail(filePath);
        } else if (fileTypeEnum == FileTypeEnum.VIDEO) {
            File tempFile = new File(appConfig.getProjectFolder() + FileConstant.FILE_FOLDER_TEMP + RandomUtil.randomString(30));
            file.transferTo(tempFile);
            filePath = filePath + FileConstant.VIDEO_SUFFIX;
            fFmpegComponent.transferVideoType(tempFile, filePath, fileSuffix);
            fFmpegComponent.createImageThumbnail(filePath);
        } else {
            filePath = filePath + fileSuffix;
            file.transferTo(new File(filePath));
        }
        ChatMessage updateEntity = new ChatMessage();
        updateEntity.setId(messageId);
        updateEntity.setStatus(MessageStatusEnum.SENT.getValue());
        updateById(updateEntity);

        MessageSendDto<Object> messageSendDto = new MessageSendDto<>();
        messageSendDto.setMeetingId(meetingId);
        messageSendDto.setMessageType(MessageTypeEnum.CHAT_MEDIA_MESSAGE.getValue());
        messageSendDto.setMessageId(meetingId);
        messageSendDto.setStatus(MessageStatusEnum.SENT.getValue());
        messageSendDto.setMessageSend2Type(MessageSendTypeEnum.GROUP.getType());
        messageHandler.sendMessage(messageSendDto);
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

    private Long parseMessageId(Serializable id) {
        if (id == null) {
            return null;
        }
        if (id instanceof Number) {
            return ((Number) id).longValue();
        }
        try {
            return Long.valueOf(String.valueOf(id));
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息 ID 非法");
        }
    }

    private String resolveStorageTableName(Long sendTime) {
        if (ChatMessageTableUtil.isSameDay(sendTime, currentTimeMillis())) {
            return ChatMessageTableUtil.BASE_TABLE_NAME;
        }
        return ChatMessageTableUtil.resolveTableName(sendTime);
    }

    private boolean updateStoredMessage(String tableName, ChatMessage chatMessage) {
        if (ChatMessageTableUtil.BASE_TABLE_NAME.equals(tableName)) {
            return baseMapper.updateById(chatMessage) > 0;
        }
        return chatMessageMapper.updatePartitionById(tableName, chatMessage) > 0;
    }

    private void insertStoredMessage(String tableName, ChatMessage chatMessage) {
        if (ChatMessageTableUtil.BASE_TABLE_NAME.equals(tableName)) {
            if (baseMapper.insert(chatMessage) <= 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "新增聊天消息失败");
            }
            return;
        }
        chatMessagePartitionManager.ensurePartitionTable(tableName);
        if (chatMessageMapper.insertPartition(tableName, chatMessage) <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "新增聊天消息失败");
        }
    }

    private boolean deleteStoredMessage(String tableName, Long messageId) {
        if (ChatMessageTableUtil.BASE_TABLE_NAME.equals(tableName)) {
            return baseMapper.deleteById(messageId) > 0;
        }
        return chatMessageMapper.deletePartitionById(tableName, messageId) > 0;
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

    private void validatePrivateHistoryQuery(ChatMessageQuery chatMessageQuery) {
        if (chatMessageQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分页参数不能为空");
        }
        if (chatMessageQuery.getCurrentUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前用户不能为空");
        }
        if (chatMessageQuery.getUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "userId 不能为空");
        }
        if (chatMessageQuery.getMeetingId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "meetingId 不能为空");
        }
        if (chatMessageQuery.getPageSize() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "pageSize 非法");
        }
        if (chatMessageQuery.getCurrent() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "current 非法");
        }
    }
}
