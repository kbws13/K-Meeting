package xyz.kbws.model.entity;

import lombok.Data;

import java.util.Date;

/**
 * 联系人表
 *
 * @TableName userContact
 */
@Data
public class UserContact {
    /**
     * 用户 ID
     */
    private Integer userId;

    /**
     * 联系人 ID
     */
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