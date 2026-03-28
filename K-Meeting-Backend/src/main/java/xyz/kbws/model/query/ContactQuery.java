package xyz.kbws.model.query;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.kbws.common.PageRequest;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2026/3/28
 * @description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ContactQuery extends PageRequest implements Serializable {

    private Integer userId;

    private Integer status;

    private Boolean queryUserInfo;

    private static final long serialVersionUID = -6803021826642363351L;
}
