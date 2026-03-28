package xyz.kbws.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

import lombok.Data;

/**
 * 联系人表
 *
 * @TableName userContact
 */
@TableName(value = "userContact")
@Data
public class UserContact {
    /**
     * 用户 ID
     */
    @TableId
    private Integer userId;

    /**
     * 联系人 ID
     */
    @TableId
    private Integer contactId;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 最后一次更新时间
     */
    private Date lastUpdateTime;
}