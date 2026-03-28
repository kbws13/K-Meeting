package xyz.kbws.model.obj;

import lombok.Data;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2026/3/28
 * @description:
 */
@Data
public class MeetingInviteObj implements Serializable {

    private static final long serialVersionUID = 5438636483347950117L;
    private String meetingName;
    private String inviteUserName;
    private Integer meetingId;
}
