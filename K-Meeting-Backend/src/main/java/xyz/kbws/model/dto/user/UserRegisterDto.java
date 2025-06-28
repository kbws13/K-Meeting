package xyz.kbws.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Data
public class UserRegisterDto implements Serializable {

    private String checkCodeKey;

    private String email;

    private String password;

    private String nickName;

    private String checkCode;

    private static final long serialVersionUID = -9175743604901167299L;
}
