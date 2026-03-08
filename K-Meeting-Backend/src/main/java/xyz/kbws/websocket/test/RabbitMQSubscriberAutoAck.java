package xyz.kbws.websocket.test;

import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * @author kbws
 * @date 2026/3/7
 * @description:
 */
@Slf4j
public class RabbitMQSubscriberAutoAck {
    private static final String EXCHANGE_NAME = "fanout_exchange";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
        String queueName = channel.queueDeclare().getQueue();
        // 关键一步：将队列绑定到交换机上
        channel.queueBind(queueName, EXCHANGE_NAME, "");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            try {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                log.info("收到消息: {}", message + System.currentTimeMillis());
            } catch (Exception e) {

            }
        };

        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
        });
    }
}
