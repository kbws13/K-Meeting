package xyz.kbws.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class ChatMessageArchiveScheduler {

    @Resource
    private ChatMessageService chatMessageService;

    /**
     * 每天凌晨 00:05 归档昨天及更早的热表消息。
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void archiveExpiredMessages() {
        int archivedCount = chatMessageService.archiveExpiredMessages();
        if (archivedCount > 0) {
            log.info("chatMessage 历史归档完成, archivedCount={}", archivedCount);
        }
    }
}
