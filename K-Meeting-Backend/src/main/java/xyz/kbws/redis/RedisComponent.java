package xyz.kbws.redis;

import org.springframework.stereotype.Component;
import xyz.kbws.constant.RedisConstant;
import xyz.kbws.model.vo.UserVO;

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
        redisUtils.setEx(RedisConstant.CHECK_CODE + checkCodeKey, code, RedisConstant.ONE_MIN);
        return checkCodeKey;
    }

    public String getCheckCode(String checkCodeKey) {
        return (String) redisUtils.get(RedisConstant.CHECK_CODE + checkCodeKey);
    }

    public void cleanCheckCode(String checkCodeKey) {
        redisUtils.delete(RedisConstant.CHECK_CODE + checkCodeKey);
    }

    public void saveUserVO(UserVO userVO) {
        redisUtils.setEx(RedisConstant.WS_TOKEN + userVO.getToken(), userVO, RedisConstant.DAY);
        redisUtils.setEx(RedisConstant.WS_TOKEN_USERID + userVO.getId(), userVO.getToken(), RedisConstant.DAY);
    }
}
