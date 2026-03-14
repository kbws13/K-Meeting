package xyz.kbws.websocket.netty;

import cn.hutool.json.JSONUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.kbws.constant.FileConstant;
import xyz.kbws.mapper.UserMapper;
import xyz.kbws.model.dto.meeting.PeerConnectDto;
import xyz.kbws.model.dto.message.MessageSendDto;
import xyz.kbws.model.entity.User;
import xyz.kbws.model.enums.MessageSendTypeEnum;
import xyz.kbws.model.enums.MessageTypeEnum;
import xyz.kbws.redis.RedisComponent;
import xyz.kbws.redis.entity.LoginUser;
import xyz.kbws.utils.UserIdCodec;
import xyz.kbws.websocket.ChannelContextUtil;
import xyz.kbws.websocket.message.MessageHandler;

import javax.annotation.Resource;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class HandlerWebSocket extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private MessageHandler messageHandler;


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) {
        String text = textWebSocketFrame.text();
        if (FileConstant.PING.equals(text)) {
            return;
        }
        log.info("收到消息: {}", text);

        PeerConnectDto peerConnectDto = JSONUtil.toBean(text, PeerConnectDto.class);
        LoginUser loginUser = redisComponent.getLoginUser(peerConnectDto.getToken());
        if (loginUser == null) {
            return;
        }
        MessageSendDto messageSendDto = new MessageSendDto();
        messageSendDto.setMessageType(MessageTypeEnum.PEER.getValue());

        PeerConnectDto peerMessage = new PeerConnectDto();
        peerMessage.setSignalType(peerConnectDto.getSignalType());
        peerMessage.setSignalData(peerConnectDto.getSignalData());

        messageSendDto.setMessageContent(peerMessage);
        messageSendDto.setMessageId(loginUser.getCurrentMeetingId());
        messageSendDto.setSendUserId(loginUser.getUserId());
        messageSendDto.setReceiveUserId(UserIdCodec.decode(peerConnectDto.getReceiveUserId()));
        messageSendDto.setMessageSend2Type(MessageSendTypeEnum.USER.getType());

        messageHandler.sendMessage(messageSendDto);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("有新的连接");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("断开连接");
        Integer userId = ctx.channel().attr(ChannelContextUtil.USER_ID_ATTR).get();
        if (userId == null) {
            return;
        }
        User user = new User();
        user.setId(userId);
        user.setLastOffTime(System.currentTimeMillis());
        userMapper.updateById(user);
    }
}
