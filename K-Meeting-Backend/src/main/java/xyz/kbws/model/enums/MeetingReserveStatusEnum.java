package xyz.kbws.model.enums;

import lombok.Getter;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Getter
public enum MeetingReserveStatusEnum {
    NO_START(0, "未开始"),
    FINISHED(1, "已结束"),
    ;

    private Integer value;
    private String desc;

    MeetingReserveStatusEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static MeetingReserveStatusEnum getByValue(Integer value) {
        for (MeetingReserveStatusEnum anEnum : MeetingReserveStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
