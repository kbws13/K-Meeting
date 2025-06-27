package xyz.kbws;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author kbws
 * @date 2025/6/25
 * @description:
 */
@SpringBootApplication
@MapperScan(basePackages = "xyz.kbws.mapper")
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
public class KMeetingApplication {
    public static void main(String[] args) {
        SpringApplication.run(KMeetingApplication.class, args);
    }
}
