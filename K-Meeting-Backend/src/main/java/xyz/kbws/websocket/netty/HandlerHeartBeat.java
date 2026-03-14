package xyz.kbws.websocket.netty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import xyz.kbws.websocket.ChannelContextUtil;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Slf4j
public class HandlerHeartBeat extends ChannelDuplexHandler {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                Integer userId = ctx.channel().attr(ChannelContextUtil.USER_ID_ATTR).get();
                log.info("用户{}没有发送心跳，断开连接", userId);
                ctx.close();
            } else if (idleStateEvent.state() == IdleState.WRITER_IDLE) {
                ctx.writeAndFlush("heart");
            }
        }
    }
}
