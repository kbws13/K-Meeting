package xyz.kbws.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Data
@Component
public class AppConfig {
    
    @Value("${ws.prot}")
    private Integer port;
}
