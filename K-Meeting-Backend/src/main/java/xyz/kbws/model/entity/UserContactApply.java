package xyz.kbws.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

import lombok.Data;

/**
 * 联系人申请表
 *
 * @TableName userContactApply
 */
@TableName(value = "userContactApply")
@Data
public class UserContactApply {
    /**
     * 申请 ID
     */
    @TableId
    private Integer id;

    /**
     * 申请人 ID
     */
    private Integer applyUserId;

    /**
     * 接收人 ID
     */
    private Integer receiveUserId;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 最后一次申请时间
     */
    private Date lastApplyTime;
}