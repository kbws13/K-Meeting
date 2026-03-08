package xyz.kbws.websocket.message;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import xyz.kbws.constant.MessageConstant;
import xyz.kbws.model.dto.message.MessageSendDto;
import xyz.kbws.websocket.ChannelContextUtil;

import javax.annotation.Resource;

/**
 * @author kbws
 * @date 2026/3/7
 * @description:
 */
@Slf4j
@Component
@ConditionalOnProperty(name = MessageConstant.HANDLE_CHANNEL_KEY, havingValue = MessageConstant.HANDLE_CHANNEL_REDIS)
public class RedisMessageHandler implements MessageHandler {

    private static final String MESSAGE_TOPIC = "message.topic";

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private ChannelContextUtil channelContextUtil;

    @Override
    public void listenMessage() {
        log.info("正在开启 Redis 消息监听，Topic: {}", MESSAGE_TOPIC);
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        // 显式指定接收 String 类型，避免 Redisson 自动反序列化 DTO 失败
        rTopic.addListener(String.class, (channel, messageJson) -> {
            try {
                log.info("Redis 订阅收到原始 JSON: {}", messageJson);
                MessageSendDto sendDto = JSONUtil.toBean(messageJson, MessageSendDto.class);
                channelContextUtil.sendMessage(sendDto);
            } catch (Exception e) {
                log.error("Redis 消息解析异常, 原始消息: {}", messageJson, e);
            }
        });
    }

    @Override
    public void sendMessage(MessageSendDto messageSendDto) {
        String messageJson = JSONUtil.toJsonStr(messageSendDto);
        log.info("准备通过 Redis 发布消息 JSON: {}", messageJson);
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        long receivedCount = rTopic.publish(messageJson);
        log.info("Redis 发布消息成功, 订阅客户端数量: {}", receivedCount);
    }
}
