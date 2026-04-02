package xyz.kbws.model.query;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.kbws.common.PageRequest;

import java.io.Serializable;

/**
 * 聊天消息分页查询参数
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChatMessageQuery extends PageRequest implements Serializable {

    /**
     * 当前登录用户 ID，服务端注入
     */
    private Integer currentUserId;

    /**
     * 对方用户 ID
     */
    private Integer userId;

    /**
     * 查询的会议 ID
     */
    private Integer meetingId;

    /**
     * 上一批查询结果中最老的消息 ID，作为继续向前翻页的游标
     */
    private Long maxMessageId;

    private static final long serialVersionUID = 7640537073569284247L;
}
