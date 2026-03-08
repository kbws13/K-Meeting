package xyz.kbws.websocket.message;

import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import xyz.kbws.constant.MessageConstant;
import xyz.kbws.model.dto.message.MessageSendDto;
import xyz.kbws.websocket.ChannelContextUtil;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * @author kbws
 * @date 2026/3/7
 * @description:
 */
@Slf4j
@Component
@ConditionalOnProperty(name = MessageConstant.HANDLE_CHANNEL_KEY, havingValue = MessageConstant.HANDLE_CHANNEL_RABBITMQ)
public class MQMessageHandler implements MessageHandler {

    @Resource
    private ChannelContextUtil channelContextUtil;

    @Value("${spring.rabbitmq.host:localhost}")
    private String host;

    @Value("${spring.rabbitmq.port:5672}")
    private int port;

    @Value("${spring.rabbitmq.username:guest}")
    private String username;

    @Value("${spring.rabbitmq.password:guest}")
    private String password;

    private static final String EXCHANGE_NAME = "fanout_exchange";
    private static final Integer MAX_RETRYTIMES = 3;
    private static final String RETRY_COUNT_KEY = "retry_count";

    private Connection connection;
    private Channel channel;

    @PostConstruct
    public void init() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setPort(port);
            factory.setUsername(username);
            factory.setPassword(password);
            this.connection = factory.newConnection();
            this.channel = connection.createChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
        } catch (Exception e) {
            log.error("RabbitMQ 连接初始化失败", e);
        }
    }

    @Override
    public void listenMessage() {
        try {
            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, EXCHANGE_NAME, "");
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                try {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    log.info("RabbitMQ 收到消息: {}", message);
                    MessageSendDto sendDto = JSONUtil.toBean(message, MessageSendDto.class);
                    channelContextUtil.sendMessage(sendDto);
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } catch (Exception e) {
                    log.error("RabbitMQ 处理消息失败", e);
                    handleFailMessage(channel, delivery, queueName);
                }
            };
            channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
            });
        } catch (Exception e) {
            log.error("RabbitMQ 监听消息失败", e);
        }
    }

    @Override
    public void sendMessage(MessageSendDto messageSendDto) {
        try (Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
            String message = JSONUtil.toJsonStr(messageSendDto);
            channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes());
            log.info("RabbitMQ 发送消息成功: {}", message);
        } catch (Exception e) {
            log.error("RabbitMQ 发送消息失败", e);
        }
    }

    private static void handleFailMessage(Channel channel, Delivery delivery, String queueName) throws IOException {
        Map<String, Object> headers = delivery.getProperties().getHeaders();
        if (headers == null) {
            headers = new HashMap<>();
        }
        int retryCount = 0;
        if (headers.containsKey(RETRY_COUNT_KEY)) {
            retryCount = (int) headers.get(RETRY_COUNT_KEY);
        }
        if (retryCount < MAX_RETRYTIMES - 1) {
            headers.put(RETRY_COUNT_KEY, retryCount + 1);
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder().headers(headers).build();
            channel.basicPublish("", queueName, properties, delivery.getBody());
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        } else {
            log.info("超过最大重试次数, 放弃处理");
            channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
        }
    }

    @PreDestroy
    void destroy() throws IOException, TimeoutException {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
    }
}

