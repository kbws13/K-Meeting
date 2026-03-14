package xyz.kbws.model.obj;

import lombok.Data;

import java.util.List;

/**
 * @author kbws
 * @date 2026/3/14
 * @description:
 */
@Data
public class MeetingExitObj {
    private Integer exitUserId;

    private List<MeetingMemberObj> meetingMemberObjList;

    private Integer exitStatus;

}
