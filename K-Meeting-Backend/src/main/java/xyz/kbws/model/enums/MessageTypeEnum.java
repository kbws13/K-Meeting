package xyz.kbws.model.enums;

import lombok.Getter;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Getter
public enum MessageTypeEnum {
    USER(0, "个人"),
    GROUP(1, "群")
    ;

    private Integer type;
    
    private String desc;

    MessageTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }
}
