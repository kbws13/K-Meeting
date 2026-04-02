package xyz.kbws.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;
import xyz.kbws.model.entity.ChatMessage;
import xyz.kbws.model.query.ChatMessageQuery;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;

/**
 * @author housenyao
 * @description 针对表【chatMessage(聊天消息表)】的数据库操作Service
 * @createDate 2026-03-28 18:54:48
 */
public interface ChatMessageService extends IService<ChatMessage> {

    /**
     * 分页查询某场会议内与指定用户的私聊记录。
     */
    Page<ChatMessage> findPrivateHistoryByPage(ChatMessageQuery chatMessageQuery);

    /**
     * 按 sendTime 路由到当天分表保存消息。
     */
    boolean savePartitioned(ChatMessage chatMessage);

    /**
     * 按会议 ID 聚合查询历史消息。
     */
    List<ChatMessage> listHistoryByMeetingId(Integer meetingId,
                                             Long startSendTime,
                                             Long endSendTime,
                                             Long beforeSendTime,
                                             Integer limit);

    /**
     * 在所有历史分表里按消息 ID 查询。
     */
    ChatMessage getByIdAcrossPartitions(Long id);

    /**
     * 把 chatMessage 热表中的历史消息归档到对应日期分表。
     *
     * @return 本次归档的消息数量
     */
    int archiveExpiredMessages();

    void saveChatMessage(ChatMessage chatMessage);
    
    void uploadFile(MultipartFile file, Integer meetingId, Long messageId, Long sendTime) throws IOException;
}
