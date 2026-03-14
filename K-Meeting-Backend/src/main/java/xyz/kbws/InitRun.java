package xyz.kbws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import xyz.kbws.websocket.message.MessageHandler;
import xyz.kbws.websocket.netty.NettyWebSocketStarter;

import javax.annotation.Resource;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Slf4j
@Component
public class InitRun implements ApplicationRunner {

    @Resource
    private NettyWebSocketStarter nettyWebSocketStarter;

    @Resource
    private MessageHandler messageHandler;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("应用启动成功，正在初始化系统组件...");

        // 启动 Netty
        new Thread(nettyWebSocketStarter).start();
        log.info("Netty WebSocket 服务已启动");

        // 启动消息监听
        try {
            messageHandler.listenMessage();
            log.info("消息监听器（MessageHandler）初始化成功");
        } catch (Exception e) {
            log.error("消息监听器初始化失败", e);
        }
    }
}
