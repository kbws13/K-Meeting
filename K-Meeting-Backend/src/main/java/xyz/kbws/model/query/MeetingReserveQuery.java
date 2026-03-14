package xyz.kbws.model.query;

import lombok.Data;
import xyz.kbws.common.PageRequest;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2026/3/14
 * @description:
 */
@Data
public class MeetingReserveQuery extends PageRequest implements Serializable {

    Integer userId;

    Boolean queryUserInfo;

    Integer status;

    String startTimeStart;

    String startTimeEnd;

    private static final long serialVersionUID = 5828487704000746196L;
}
