package xyz.kbws.redis.entity;

import lombok.Data;
import xyz.kbws.model.vo.UserVO;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2026/3/7
 * @description:
 */
@Data
public class LoginUser extends UserVO implements Serializable {
    
    private Integer userId;
    
    private static final long serialVersionUID = -5558882804225519592L;
}
