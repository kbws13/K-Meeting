package xyz.kbws.model.obj;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author kbws
 * @date 2025/9/8
 * @description:
 */
@Data
public class MeetingJoinObj implements Serializable {
    private MeetingMemberObj newMember;
    
    private List<MeetingMemberObj> meetingMemberList;
}
