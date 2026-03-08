package xyz.kbws.redis;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import xyz.kbws.constant.MessageConstant;

/**
 * @author kbws
 * @date 2026/3/7
 * @description:
 */
@Slf4j
@Component
@ConditionalOnProperty(name = MessageConstant.HANDLE_CHANNEL_KEY, havingValue = MessageConstant.HANDLE_CHANNEL_REDIS)
public class RedissonConfig {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private String redisPort;

    @Bean(name = "redissonClient", destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        try {
            Config config = new Config();
            config.useSingleServer().setAddress("redis://" + redisHost + ":" + redisPort);
            return Redisson.create(config);
        } catch (Exception e) {
            log.error("Redis 配置错误, 请检查 Redis 配置", e);
        }
        return null;
    }
}
