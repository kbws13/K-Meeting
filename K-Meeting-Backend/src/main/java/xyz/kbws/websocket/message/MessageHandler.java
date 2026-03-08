package xyz.kbws.websocket.message;

import org.springframework.stereotype.Component;
import xyz.kbws.model.dto.message.MessageSendDto;

/**
 * @author kbws
 * @date 2026/3/7
 * @description:
 */
public interface MessageHandler {

    void listenMessage();

    void sendMessage(MessageSendDto messageSendDto);
}
