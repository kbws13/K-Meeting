package xyz.kbws.model.dto.meeting;

import lombok.Data;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2026/3/7
 * @description:
 */
@Data
public class PreJoinDto implements Serializable {

    private Integer meetingNo;

    private String nickName;

    private String password;

    private static final long serialVersionUID = -548802103744951434L;
}
