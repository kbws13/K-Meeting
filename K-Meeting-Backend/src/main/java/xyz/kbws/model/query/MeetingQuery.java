package xyz.kbws.model.query;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.kbws.common.PageRequest;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2025/7/5
 * @description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MeetingQuery extends PageRequest implements Serializable {
    private Integer userId;
    private Boolean queryMeetingMemberCount;
    private static final long serialVersionUID = 4502089952485377344L;
}
