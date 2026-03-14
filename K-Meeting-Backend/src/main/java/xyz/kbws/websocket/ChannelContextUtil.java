package xyz.kbws.websocket;

import com.alibaba.fastjson.JSON;
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
import xyz.kbws.model.enums.MessageSendTypeEnum;
import xyz.kbws.redis.RedisComponent;
import xyz.kbws.redis.entity.LoginUser;

import javax.annotation.Resource;
import java.util.concurrent.ConcurrentHashMap;

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
        channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(messageSendDto)));
    }

    private void sendMessage2Group(MessageSendDto messageSendDto) {
        if (messageSendDto.getMeetingId() == null) {
            return;
        }
        ChannelGroup group = MEETING_ROOM_CONTEXT_MAP.get(messageSendDto.getMeetingId());
        if (group == null) {
            return;
        }
        group.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(messageSendDto)));
    }
}
