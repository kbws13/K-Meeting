package xyz.kbws.model.enums;

import lombok.Getter;

/**
 * @author kbws
 * @date 2026/4/6
 * @description:
 */
@Getter
public enum AppUpdateStatusEnum {
    DISABLE(0, "停用"),
    ENABLE(1, "启用"),
    ;

    private final Integer value;

    private final String desc;

    AppUpdateStatusEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static AppUpdateStatusEnum getByValue(Integer value) {
        for (AppUpdateStatusEnum anEnum : AppUpdateStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
