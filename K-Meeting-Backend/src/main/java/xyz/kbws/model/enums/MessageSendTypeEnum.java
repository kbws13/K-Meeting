package xyz.kbws.model.enums;

import lombok.Getter;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Getter
public enum MessageSendTypeEnum {
    USER(0, "个人"),
    GROUP(1, "群")
    ;

    private Integer type;
    
    private String desc;

    MessageSendTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }
}
