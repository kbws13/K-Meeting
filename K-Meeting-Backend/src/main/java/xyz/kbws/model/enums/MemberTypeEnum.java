package xyz.kbws.model.enums;

import lombok.Getter;

/**
 * @author kbws
 * @date 2026/3/7
 * @description:
 */
@Getter
public enum MemberTypeEnum {
    NORMAL(0, "普通成员"),
    COMPERE(1, "主持人"),
    ;

    private final Integer value;
    private final String desc;

    MemberTypeEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static MemberTypeEnum getByValue(Integer value) {
        for (MemberTypeEnum anEnum : MemberTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
