package xyz.kbws.model.enums;

import lombok.Getter;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Getter
public enum ContactApplyStatusEnum {
    INIT(0, "待处理"),
    PASS(1, "已同意"),
    REJECT(2, "已拒绝"),
    BLACKLIST(3, "已拉黑"),
    ;

    private Integer value;
    private String desc;

    ContactApplyStatusEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static ContactApplyStatusEnum getByValue(Integer value) {
        for (ContactApplyStatusEnum anEnum : ContactApplyStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
