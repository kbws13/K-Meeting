package xyz.kbws.model.query;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.kbws.common.PageRequest;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2026/4/6
 * @description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserQuery extends PageRequest implements Serializable {

    private String email;

    private String nickName;

    private Integer status;

    private String userRole;

    private static final long serialVersionUID = 3217330939463554453L;
}
