package xyz.kbws.websocket;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.kbws.mapper.UserMapper;
import xyz.kbws.model.dto.message.MessageSendDto;
import xyz.kbws.model.entity.User;
import xyz.kbws.model.enums.MeetingMemberStatus;
import xyz.kbws.model.enums.MessageSendTypeEnum;
import xyz.kbws.model.enums.MessageTypeEnum;
import xyz.kbws.model.obj.MeetingExitObj;
import xyz.kbws.model.obj.MeetingJoinObj;
import xyz.kbws.model.obj.MeetingMemberObj;
import xyz.kbws.redis.RedisComponent;
import xyz.kbws.redis.entity.LoginUser;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Slf4j
@Component
public class ChannelContextUtil {

    public static final AttributeKey<Integer> USER_ID_ATTR = AttributeKey.valueOf("userId");

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private UserMapper userMapper;

    public static final ConcurrentHashMap<Integer, Channel> USER_CONTEXT_MAP = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Integer, ChannelGroup> MEETING_ROOM_CONTEXT_MAP = new ConcurrentHashMap<>();

    public void addContext(Integer userId, Channel channel) {
        try {
            channel.attr(USER_ID_ATTR).set(userId);
            USER_CONTEXT_MAP.put(userId, channel);
            User user = new User();
            user.setId(userId);
            user.setLastLoginTime(System.currentTimeMillis());
            userMapper.updateById(user);

            LoginUser loginUser = redisComponent.getLoginUserById(userId);
            if (loginUser == null || loginUser.getCurrentMeetingId() == null) {
                return;
            }
            // 自动加入会议
            addMeetingRoom(loginUser.getCurrentMeetingId(), userId);
        } catch (Exception e) {
            log.error("初始化连接失败: {}", e.getMessage());
        }
    }

    public void addMeetingRoom(Integer meetingId, Integer userId) {
        Channel userChannel = USER_CONTEXT_MAP.get(userId);
        if (userChannel == null) {
            return;
        }
        ChannelGroup channelGroup = MEETING_ROOM_CONTEXT_MAP.get(meetingId);
        if (channelGroup == null) {
            channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
            MEETING_ROOM_CONTEXT_MAP.put(meetingId, channelGroup);
        }
        Channel channel = channelGroup.find(userChannel.id());
        if (channel == null) {
            channelGroup.add(userChannel);
        }
    }

    public void sendMessage(MessageSendDto messageSendDto) {
        if (MessageSendTypeEnum.USER.getType().equals(messageSendDto.getMessageSend2Type())) {
            sendMessage2User(messageSendDto);
        } else {
            sendMessage2Group(messageSendDto);
        }
    }

    public void closeContext(Integer userId) {
        if (userId == null) {
            return;
        }
        Channel channel = USER_CONTEXT_MAP.remove(userId);
        if (channel != null) {
            channel.close();
        }
    }

    private void sendMessage2User(MessageSendDto messageSendDto) {
        if (messageSendDto.getReceiveUserId() == null) {
            return;
        }
        Channel channel = USER_CONTEXT_MAP.get(messageSendDto.getReceiveUserId());
        if (channel == null) {
            return;
        }
        channel.writeAndFlush(new TextWebSocketFrame(buildWsMessageJson(messageSendDto)));
    }

    private void sendMessage2Group(MessageSendDto messageSendDto) {
        if (messageSendDto.getMeetingId() == null) {
            return;
        }
        ChannelGroup group = MEETING_ROOM_CONTEXT_MAP.get(messageSendDto.getMeetingId());
        if (group == null) {
            return;
        }
        group.writeAndFlush(new TextWebSocketFrame(buildWsMessageJson(messageSendDto)));

        if (MessageTypeEnum.EXIT_MEETING_ROOM.getValue().equals(messageSendDto.getMessageType())) {
            MeetingExitObj exitObj = JSONUtil.toBean((String) messageSendDto.getMessageContent(), MeetingExitObj.class);
            removeContextFromGroup(exitObj.getExitUserId(), messageSendDto.getMeetingId());
            List<MeetingMemberObj> meetingMemberObjList = redisComponent.getMeetingMemberList(messageSendDto.getMeetingId());
            List<MeetingMemberObj> onLineMemberList = meetingMemberObjList.stream().filter(item -> MeetingMemberStatus.NORMAL.getStatus().equals(item.getStatus())).collect(Collectors.toList());
            if (onLineMemberList.isEmpty()) {
                removeContextGroup(messageSendDto.getMeetingId());
            }
        }
        if (MessageTypeEnum.FINISH_MEETING.getValue().equals(messageSendDto.getMessageType())) {
            List<MeetingMemberObj> meetingMemberObjList = redisComponent.getMeetingMemberList(messageSendDto.getMeetingId());
            for (MeetingMemberObj meetingMemberObj : meetingMemberObjList) {
                removeContextFromGroup(meetingMemberObj.getUserId(), messageSendDto.getMeetingId());
            }
            removeContextGroup(messageSendDto.getMeetingId());
        }
    }

    private void removeContextFromGroup(Integer userId, Integer meetingId) {
        Channel context = USER_CONTEXT_MAP.get(userId);
        if (context == null) {
            return;
        }
        ChannelGroup channelGroup = MEETING_ROOM_CONTEXT_MAP.get(meetingId);
        if (channelGroup != null) {
            channelGroup.remove(context);
        }
    }

    private void removeContextGroup(Integer meetingId) {
        MEETING_ROOM_CONTEXT_MAP.remove(meetingId);
    }

    private String buildWsMessageJson(MessageSendDto<?> messageSendDto) {
        JSONObject payload = (JSONObject) JSON.toJSON(messageSendDto);
        if (messageSendDto.getSendUserId() != null) {
            // TODO 暂时停止混淆
            payload.put("sendUserId", messageSendDto.getSendUserId());
        }
        if (messageSendDto.getReceiveUserId() != null) {
            // TODO 暂时停止混淆
            payload.put("receiveUserId", messageSendDto.getReceiveUserId());
        }
        if (messageSendDto.getMessageId() != null) {
            payload.put("messageId", String.valueOf(messageSendDto.getMessageId()));
        }

        Object messageContent = messageSendDto.getMessageContent();
        if (messageContent instanceof MeetingJoinObj) {
            payload.put("messageContent", buildMeetingJoinPayload((MeetingJoinObj) messageContent));
        } else if (MessageTypeEnum.EXIT_MEETING_ROOM.getValue().equals(messageSendDto.getMessageType())
                && messageContent instanceof String) {
            MeetingExitObj meetingExitObj = JSONUtil.toBean((String) messageContent, MeetingExitObj.class);
            payload.put("messageContent", buildMeetingExitPayload(meetingExitObj));
        }
        return JSON.toJSONString(payload);
    }

    private JSONObject buildMeetingJoinPayload(MeetingJoinObj meetingJoinObj) {
        JSONObject payload = new JSONObject();
        payload.put("newMember", buildMeetingMemberPayload(meetingJoinObj.getNewMember()));
        List<JSONObject> memberList = new ArrayList<>();
        if (meetingJoinObj.getMeetingMemberList() != null) {
            for (MeetingMemberObj meetingMemberObj : meetingJoinObj.getMeetingMemberList()) {
                memberList.add(buildMeetingMemberPayload(meetingMemberObj));
            }
        }
        payload.put("meetingMemberList", memberList);
        return payload;
    }

    private JSONObject buildMeetingExitPayload(MeetingExitObj meetingExitObj) {
        JSONObject payload = new JSONObject();
        if (meetingExitObj.getExitUserId() != null) {
            // TODO 暂时停止混淆
            payload.put("exitUserId", meetingExitObj.getExitUserId());
        }
        payload.put("exitStatus", meetingExitObj.getExitStatus());
        List<JSONObject> memberList = new ArrayList<>();
        if (meetingExitObj.getMeetingMemberObjList() != null) {
            for (MeetingMemberObj meetingMemberObj : meetingExitObj.getMeetingMemberObjList()) {
                memberList.add(buildMeetingMemberPayload(meetingMemberObj));
            }
        }
        payload.put("meetingMemberObjList", memberList);
        return payload;
    }

    private JSONObject buildMeetingMemberPayload(MeetingMemberObj meetingMemberObj) {
        if (meetingMemberObj == null) {
            return null;
        }
        JSONObject payload = (JSONObject) JSON.toJSON(meetingMemberObj);
        if (meetingMemberObj.getUserId() != null) {
            // TODO 暂时停止混淆
            payload.put("userId", meetingMemberObj.getUserId());
        }
        return payload;
    }
}
