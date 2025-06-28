package xyz.kbws.websocket.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class HandlerWebSocket extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) throws Exception {
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
        // TODO 处理断开连接的逻辑
    }
}
