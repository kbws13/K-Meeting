package xyz.kbws.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author kbws
 * @date 2026/4/6
 * @description:
 */
@Data
public class UserAdminVO implements Serializable {

    private String userId;

    private String email;

    private String nickName;

    private Integer sex;

    private Integer status;

    private String meetingNo;

    private String userRole;

    private Date createTime;

    private Date updateTime;

    private Long lastLoginTime;

    private Long lastOffTime;

    private static final long serialVersionUID = -1313023259941456440L;
}
