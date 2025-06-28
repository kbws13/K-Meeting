package xyz.kbws.websocket.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.kbws.config.AppConfig;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Slf4j
@Component
public class NettyWebSocketStarter implements Runnable{
    
    @Resource
    private AppConfig appConfig;

    /**
     * boos 线程组，用于处理连接
     */
    private EventLoopGroup boosGroup = new NioEventLoopGroup();
    /**
     * worker 线程组，用于处理消息
     */
    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    
    @Resource
    private HandlerTokenValidation handlerTokenValidation;

    @Resource
    private HandlerWebSocket handlerWebSocket;
    
    @Override
    public void run() {
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(boosGroup, workerGroup);
            serverBootstrap.channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            ChannelPipeline pipeline = channel.pipeline();
                            // 对 HTTP 协议的支持，使用 HTTP 的编码器、解码器
                            pipeline.addLast(new HttpServerCodec());
                            // HTTP 消息聚合器，将分片的 HTTP 消息聚合成完成 FullHttpRequest、FullHttpResponse
                            pipeline.addLast(new HttpObjectAggregator(64 * 1024));
                            pipeline.addLast(new IdleStateHandler(6, 0, 0));
                            pipeline.addLast(new HandlerHeartBeat());
                            // token 校验
                            pipeline.addLast(handlerTokenValidation);
                            pipeline.addLast(new WebSocketServerProtocolHandler("/ws", null, true, 6553, true, true, 10000L));
                            pipeline.addLast(handlerWebSocket);
                        }
                    });
            Channel channel = serverBootstrap.bind(appConfig.getPort()).sync().channel();
            log.info("Netty 启动成功, 端口: {}", appConfig.getPort());
            channel.closeFuture().sync();
        } catch (Exception e) {
            log.error("Netty 启动失败: {}", e.getMessage());
        } finally {
            boosGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    @PreDestroy
    private void dispose() {
        boosGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
