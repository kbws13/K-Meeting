package xyz.kbws.model.enums;

import lombok.Getter;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Getter
public enum MessageStatusEnum {
    SENDING(0, "正在发送"),
    SENT(1, "已发送"),
    ;

    private Integer value;
    private String desc;

    MessageStatusEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static MessageStatusEnum getByValue(Integer value) {
        for (MessageStatusEnum anEnum : MessageStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
