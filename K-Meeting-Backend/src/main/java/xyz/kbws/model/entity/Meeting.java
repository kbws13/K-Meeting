package xyz.kbws.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 会议表
 * @TableName meeting
 */
@TableName(value ="meeting")
@Data
public class Meeting {
    /**
     * 会议 ID
     */
    @TableId
    private Integer id;

    /**
     * 会议号
     */
    private String meetingNo;

    /**
     * 会议名
     */
    private String name;

    /**
     * 创建人 ID
     */
    private Integer createUserId;

    /**
     * 加入方式
     */
    private Integer joinType;

    /**
     * 加入密码
     */
    private String joinPassword;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

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

    /**
     * 成员数量
     */
    @TableField(exist = false)
    private Integer memberCount;
}