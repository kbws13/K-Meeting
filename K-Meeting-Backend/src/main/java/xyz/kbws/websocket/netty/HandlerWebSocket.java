package xyz.kbws.websocket.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.kbws.mapper.UserMapper;
import xyz.kbws.model.entity.User;

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
    
    
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) {
        String text = textWebSocketFrame.text();
        log.info("收到消息: {}", text);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("有新的连接");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("断开连接");
        Attribute<String> attribute = ctx.channel().attr(AttributeKey.valueOf(ctx.channel().id().toString()));
        String userId = attribute.get();
        User user = new User();
        user.setId(Integer.parseInt(userId));
        user.setLastOffTime(System.currentTimeMillis());
        userMapper.updateById(user);
    }
}
