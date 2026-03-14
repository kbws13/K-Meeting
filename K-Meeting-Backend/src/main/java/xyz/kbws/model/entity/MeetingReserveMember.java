package xyz.kbws.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 预约会议成员表
 *
 * @TableName meetingReserveMember
 */
@TableName(value = "meetingReserveMember")
@Data
public class MeetingReserveMember {
    /**
     * 会议 ID
     */
    private Integer meetingId;

    /**
     * 邀请用户 ID
     */
    private Integer inviteUserId;
}