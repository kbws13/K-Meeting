package xyz.kbws.websocket;

import cn.hutool.core.util.StrUtil;
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
import xyz.kbws.model.vo.UserVO;
import xyz.kbws.redis.RedisComponent;

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

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private UserMapper userMapper;

    public static final ConcurrentHashMap<Integer, Channel> USER_CONTEXT_MAP = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, ChannelGroup> MEETING_ROOM_CONTEXT_MAP = new ConcurrentHashMap<>();

    public void addContext(Integer userId, Channel channel) {
        try {
            String channelId = channel.id().toString();
            AttributeKey attributeKey;
            if (!AttributeKey.exists(channelId)) {
                attributeKey = AttributeKey.newInstance(channelId);
            } else {
                attributeKey = AttributeKey.valueOf(channelId);
            }
            channel.attr(attributeKey).set(userId);
            USER_CONTEXT_MAP.put(userId, channel);
            User user = new User();
            user.setId(userId);
            user.setLastLoginTime(System.currentTimeMillis());
            userMapper.updateById(user);

            UserVO userVO = redisComponent.getLoginUserById(userId);
            if (userVO.getCurrentMeetingId() == null) {
                return;
            }
            // 自动加入会议
            addMeetingRoom(String.valueOf(userVO.getCurrentMeetingId()), userId);
        } catch (Exception e) {
            log.error("初始化连接失败: {}", e.getMessage());
        }
    }

    public void addMeetingRoom(String meetingId, Integer userId) {
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

    public void closeContext(String userId) {
        if (StrUtil.isEmpty(userId)) {
            return;
        }
        Channel channel = USER_CONTEXT_MAP.get(Integer.parseInt(userId));
        USER_CONTEXT_MAP.remove(Integer.parseInt(userId));
        if (channel != null) {
            channel.close();
        }
    }

    private void sendMessage2User(MessageSendDto messageSendDto) {
        if (messageSendDto.getReceiveUserId() == null) {
            return;
        }
        Channel channel = USER_CONTEXT_MAP.get(Integer.parseInt(messageSendDto.getReceiveUserId()));
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
