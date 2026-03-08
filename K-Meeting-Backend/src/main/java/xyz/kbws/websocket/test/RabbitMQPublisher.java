package xyz.kbws.websocket.test;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * @author kbws
 * @date 2026/3/7
 * @description:
 */
public class RabbitMQPublisher {
    private static final String EXCHANGE_NAME = "fanout_exchange";

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");

        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
            String message = "RabbitMQ 发送消息测试";
            channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes());
        } catch (Exception e) {
            
        }
    }
}
