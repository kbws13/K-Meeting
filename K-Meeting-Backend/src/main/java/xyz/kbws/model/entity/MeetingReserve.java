package xyz.kbws.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 预约会议表
 *
 * @TableName meetingReserve
 */
@TableName(value = "meetingReserve")
@Data
public class MeetingReserve {
    /**
     * 会议 ID
     */
    @TableId
    private Integer meetingId;

    /**
     * 会议名
     */
    private String name;

    /**
     * 加入方式
     */
    private Integer joinType;

    /**
     * 加入密码
     */
    private String joinPassword;

    /**
     * 会议时长
     */
    private Integer duration;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 创建人 ID
     */
    private Integer createUserId;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}