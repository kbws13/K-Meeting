package xyz.kbws.model.enums;

import lombok.Getter;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Getter
public enum MessageTypeEnum {
    INIT(0, "连接 WS 获取信息"),
    ADD_MEETING_ROOM(1, "加入房间"),
    PEER(2, "发送 peer"),
    EXIT_MEETING_ROOM(3, "退出房间"),
    FINISH_MEETING(4, "结束会议"),
    CHAT_TEXT_MESSAGE(5, "文本消息"),
    CHAT_MEDIA_MESSAGE(6, "媒体消息"),
    CHAT_MEDIA_MESSAGE_UPDATE(7, "媒体消息更新"),
    USER_CONTACT_APPLY(8, "好友申请消息"),
    INVITE_MEMBER_MEETING(9, "邀请入会"),
    FORCE_OFF_LINE(10, "强制下线"),
    MEETING_USER_VIDEO_CHANGE(11, "用户视频改变"),
    USER_CONTACT_ACCESS(12, "好友申请已处理")
    ;

    private Integer value;
    private String desc;

    MessageTypeEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static MessageTypeEnum getByValue(Integer value) {
        for (MessageTypeEnum anEnum : MessageTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
