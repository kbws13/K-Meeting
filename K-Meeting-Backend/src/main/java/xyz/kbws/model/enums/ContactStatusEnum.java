package xyz.kbws.model.enums;

import lombok.Getter;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Getter
public enum ContactStatusEnum {
    FRIEND(0, "好友"),
    DELETE(1, "已删除好友"),
    BLACKLIST(2, "已拉黑"),
    ;

    private Integer value;
    private String desc;

    ContactStatusEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static ContactStatusEnum getByValue(Integer value) {
        for (ContactStatusEnum anEnum : ContactStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
