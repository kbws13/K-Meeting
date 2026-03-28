package xyz.kbws.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 聊天消息表
 * @TableName chatMessage
 */
@TableName(value ="chatMessage")
@Data
public class ChatMessage {
    /**
     * 消息 ID
     */
    @TableId
    private Long id;

    /**
     * 会议 ID
     */
    private Integer meetingId;

    /**
     * 消息类型
     */
    private Integer type;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 发送人 ID
     */
    private Integer sendUserId;

    /**
     * 发送人昵称
     */
    private String sendUserNickName;

    /**
     * 发送时间
     */
    private Long sendTime;

    /**
     * 发送类型
     */
    private Integer receiveType;

    /**
     * 接收用户 id
     */
    private Integer receiveUserId;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件类型
     */
    private Integer fileType;

    /**
     * 文件后缀
     */
    private String fileSuffix;

    /**
     * 状态
     */
    private Integer status;
}