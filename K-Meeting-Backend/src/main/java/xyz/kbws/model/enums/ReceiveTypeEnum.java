package xyz.kbws.model.enums;

import lombok.Getter;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Getter
public enum ReceiveTypeEnum {
    ALL(0, "全员"),
    USER(1, "个人"),
    ;

    private Integer value;
    private String desc;

    ReceiveTypeEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static ReceiveTypeEnum getByValue(Integer value) {
        for (ReceiveTypeEnum anEnum : ReceiveTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
