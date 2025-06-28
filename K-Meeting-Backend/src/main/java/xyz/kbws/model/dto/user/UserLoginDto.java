package xyz.kbws.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Data
public class UserLoginDto implements Serializable {
    
    private String email;
    
    private String password;
    
    private static final long serialVersionUID = -9175743604901167299L;
}
