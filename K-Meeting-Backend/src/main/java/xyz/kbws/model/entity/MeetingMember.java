package xyz.kbws.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

/**
 * 会议成员表
 * @TableName meetingMember
 */
@Data
public class MeetingMember {
    /**
     * 会议 ID
     */
    @TableId
    private Integer meetingId;

    /**
     * 用户 ID
     */
    @TableId
    private Integer userId;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 最后一次加入时间
     */
    private Date lastJoinTime;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 
     */
    private Integer memberType;

    /**
     * 会议状态
     */
    private Integer meetingStatus;
}