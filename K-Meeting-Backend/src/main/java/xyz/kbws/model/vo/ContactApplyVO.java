package xyz.kbws.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.kbws.model.entity.UserContactApply;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2026/3/28
 * @description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ContactApplyVO extends UserContactApply implements Serializable {

    private String applyContactUserId;

    private String receiveContactUserId;

    private String email;

    private String nickName;

    private Integer sex;

    private Integer userStatus;

    private String meetingNo;

    private Long lastLoginTime;

    private Long lastOffTime;

    private static final long serialVersionUID = 721204249983552529L;
}
