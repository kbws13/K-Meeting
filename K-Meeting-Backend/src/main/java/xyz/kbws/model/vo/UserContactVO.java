package xyz.kbws.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2026/3/25
 * @description:
 */
@Data
public class UserContactVO implements Serializable {

    private String userId;

    private String nickName;

    private Integer status;

    private Long lastLoginTime;

    private Long lastOffTime;

    private Integer onlineType;

    public Integer getOnlineType() {
        if (lastLoginTime != null && lastLoginTime > lastOffTime) {
            return 1;
        } else {
            return 0;
        }
    }

    private static final long serialVersionUID = 7513664475349537921L;
}
