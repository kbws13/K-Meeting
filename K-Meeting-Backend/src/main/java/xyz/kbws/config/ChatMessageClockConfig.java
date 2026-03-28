package xyz.kbws.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.kbws.utils.ChatMessageTableUtil;

import java.time.Clock;

@Configuration
public class ChatMessageClockConfig {

    @Bean
    public Clock chatMessageClock() {
        return Clock.system(ChatMessageTableUtil.TABLE_ZONE_ID);
    }
}
