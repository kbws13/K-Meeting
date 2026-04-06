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
public class AppUpdateQuery extends PageRequest implements Serializable {

    private String version;

    private Integer status;

    private Integer fileType;

    private static final long serialVersionUID = -9059836056584772467L;
}
