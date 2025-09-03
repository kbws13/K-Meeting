package xyz.kbws.model.enums;

import lombok.Getter;

/**
 * @author kbws
 * @date 2025/9/3
 * @description: 会议成员状态
 */
@Getter
public enum MeetingMemberStatus {
    DEL_MEETING(0, "删除会议"),
    NORMAL(1, "正常"),
    EXIT_MEETING(2, "退出会议"),
    KICK_OUT(3, "被踢出会议"),
    BLACKLIST(4, "被拉黑"),
    ;
    private final Integer status;
    private final String desc;

    MeetingMemberStatus(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public static MeetingMemberStatus getByStatus(Integer status) {
        for (MeetingMemberStatus item : MeetingMemberStatus.values()) {
            if (item.getStatus().equals(status)) {
                return item;
            }
        }
        return null;
    }
}
