package xyz.kbws.model.obj;

import lombok.Data;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2025/9/8
 * @description:
 */
@Data
public class MeetingMemberObj implements Serializable {
    private Integer userId;

    private String nickName;

    private String avatar;

    private Long joinTime;

    private Integer memberType;

    private Integer status;

    private Boolean openVideo;

    private Integer sex;
}
