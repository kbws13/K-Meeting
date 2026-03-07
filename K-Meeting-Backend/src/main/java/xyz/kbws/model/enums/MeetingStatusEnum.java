package xyz.kbws.model.enums;

import lombok.Getter;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Getter
public enum MeetingStatusEnum {
    PENDING(0, "会议进行中"),
    FINISHED(1, "会议已结束"),
    ;

    private Integer value;
    private String desc;

    MeetingStatusEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static MeetingStatusEnum getByValue(Integer value) {
        for (MeetingStatusEnum anEnum : MeetingStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
