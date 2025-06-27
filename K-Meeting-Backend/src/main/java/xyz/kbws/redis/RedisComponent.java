package xyz.kbws.redis;

import org.springframework.stereotype.Component;
import xyz.kbws.constant.RedisConstant;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Component
public class RedisComponent {
    @Resource
    private RedisUtil<Object> redisUtils;

    public String saveCheckCode(String code) {
        String checkCodeKey = UUID.randomUUID().toString();
        redisUtils.setEx(RedisConstant.CHECK_CODE + checkCodeKey, code, 6000 * 10);
        return checkCodeKey;
    }
}
