package xyz.kbws.constant;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
public interface RedisConstant {
    Integer ONE_MIN = 60;
    Integer DAY = ONE_MIN * 60 * 24;
    String PREFIX = "kMeeting:";
    String CHECK_CODE = PREFIX + "checkCode:";
    String WS_TOKEN = PREFIX + "ws:token";
    String WS_TOKEN_USERID = PREFIX + "ws:token:userId";
    String WS_USER_HEART_BEAT = PREFIX + "ws:user:heartbeat";
    String MEETING_ROOM = PREFIX + "meeting:room:";
    String INVITE_MEMBER = PREFIX + "meeting:invite:member:";
    String SYSTEM_SETTING = PREFIX + "systemSetting:";
    String MEETING_NO_PREFIX = "M";
    String IMAGE_THUMBNAIL_SUFFIX = "_thumbnail";
}
