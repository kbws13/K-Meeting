package xyz.kbws.websocket.netty;

import cn.hutool.core.util.StrUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.kbws.model.vo.UserVO;
import xyz.kbws.redis.RedisComponent;
import xyz.kbws.websocket.ChannelContextUtil;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class HandlerTokenValidation extends SimpleChannelInboundHandler<FullHttpRequest> {
    
    @Resource
    private RedisComponent redisComponent;
    
    @Resource
    private ChannelContextUtil channelContextUtil;
    
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) {
        String uri = fullHttpRequest.uri();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
        List<String> tokens = queryStringDecoder.parameters().get("token");
        if (tokens == null) {
            sendErrorResponse(channelHandlerContext);
            return;
        }
        String token = tokens.get(0);
        UserVO userVO = checkUserVO(token);
        if (userVO == null) {
            log.error("校验 token 失败: {}", token);
            sendErrorResponse(channelHandlerContext);
            return;
        }
        channelHandlerContext.fireChannelRead(fullHttpRequest.retain());
        channelContextUtil.addContext(String.valueOf(userVO.getId()), channelHandlerContext.channel());
    }

    private UserVO checkUserVO(String token) {
        if (StrUtil.isEmpty(token)) {
            return null;
        }
        return redisComponent.getUserVO(token);
    }

    private void sendErrorResponse(ChannelHandlerContext context) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN, Unpooled.copiedBuffer("token无效", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain;charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
