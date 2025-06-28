package xyz.kbws.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Data
public class UserVO implements Serializable {
    
    private Integer id;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 性别 0:女 1:男 2:保密
     */
    private Integer sex;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 个人会议号
     */
    private String meetingNo;
    
    private String currentMeetingNo;
    
    private String currentNickName;

    /**
     * 最后登录时间
     */
    private Long lastLoginTime;

    /**
     * 最后离开时间
     */
    private Long lastOffTime;

    private String token;

    private static final long serialVersionUID = 7731990810876289523L;
}
