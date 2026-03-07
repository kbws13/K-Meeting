package xyz.kbws.model.enums;

import lombok.Getter;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Getter
public enum MeetingJoinTypeEnum {
     NO_PASSWORD(0, "无需密码"),
    PASSWORD(1, "需要密码"),
    ;

    private Integer value;
    private String desc;

    MeetingJoinTypeEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static MeetingJoinTypeEnum getByValue(Integer value) {
        for (MeetingJoinTypeEnum anEnum : MeetingJoinTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
