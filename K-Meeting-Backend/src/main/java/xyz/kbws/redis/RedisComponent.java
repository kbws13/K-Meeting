package xyz.kbws.redis;

import org.springframework.stereotype.Component;
import xyz.kbws.constant.RedisConstant;
import xyz.kbws.model.obj.MeetingMemberObj;
import xyz.kbws.redis.entity.LoginUser;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Component
public class RedisComponent {
    @Resource
    private RedisUtil<Object> redisUtils;

    /**
     * 构建 userId key
     */
    private String buildUserIdKey(Integer userId) {
        return RedisConstant.WS_TOKEN_USERID + userId;
    }

    /**
     * 保存验证码
     */
    public String saveCheckCode(String code) {
        String checkCodeKey = UUID.randomUUID().toString();
        redisUtils.setEx(RedisConstant.CHECK_CODE + checkCodeKey, code, RedisConstant.ONE_MIN);
        return checkCodeKey;
    }

    /**
     * 获取验证码
     */
    public String getCheckCode(String checkCodeKey) {
        return (String) redisUtils.get(RedisConstant.CHECK_CODE + checkCodeKey);
    }

    /**
     * 删除验证码
     */
    public void cleanCheckCode(String checkCodeKey) {
        redisUtils.delete(RedisConstant.CHECK_CODE + checkCodeKey);
    }

    /**
     * 保存用户信息
     */
    public void saveUserVO(LoginUser loginUser) {
        String token = loginUser.getToken();
        // token -> userVO
        redisUtils.setEx(RedisConstant.WS_TOKEN + token, loginUser, RedisConstant.DAY);
        // userId -> token
        redisUtils.setEx(buildUserIdKey(loginUser.getUserId()), token, RedisConstant.DAY);
    }

    public LoginUser getLoginUser(String token) {
        return (LoginUser) redisUtils.get(RedisConstant.WS_TOKEN + token);
    }

    public LoginUser getLoginUserById(Integer userId) {
        String token = (String) redisUtils.get(buildUserIdKey(userId));
        if (token == null) {
            return null;
        }
        return getLoginUser(token);
    }

    public void resetUserVO(LoginUser loginUser) {
        this.saveUserVO(loginUser);
    }

    public void addMeeting(Integer meetingId, MeetingMemberObj meetingMemberObj) {
        redisUtils.hset(RedisConstant.MEETING_ROOM + meetingId.toString(), meetingMemberObj.getUserId().toString(), meetingMemberObj);
    }

    public List<MeetingMemberObj> getMeetingMemberList(Integer meetingId) {
        List<MeetingMemberObj> meetingMemberObjList = redisUtils.hvals(RedisConstant.MEETING_ROOM + meetingId.toString(), MeetingMemberObj.class);
        meetingMemberObjList = meetingMemberObjList.stream().sorted(Comparator.comparing(MeetingMemberObj::getJoinTime)).collect(Collectors.toList());
        return meetingMemberObjList;
    }

    public MeetingMemberObj getMeetingMember(Integer meetingId, Integer userId) {
        return (MeetingMemberObj) redisUtils.hget(RedisConstant.MEETING_ROOM + meetingId.toString(), userId.toString());
    }
}
