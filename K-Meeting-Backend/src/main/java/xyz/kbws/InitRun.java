package xyz.kbws;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import xyz.kbws.websocket.netty.NettyWebSocketStarter;

import javax.annotation.Resource;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Component
public class InitRun implements ApplicationRunner {
    
    @Resource
    private NettyWebSocketStarter nettyWebSocketStarter;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        new Thread(nettyWebSocketStarter).start();
    }
}
