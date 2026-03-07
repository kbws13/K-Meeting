package xyz.kbws.model.dto.meeting;

import lombok.Data;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2025/9/8
 * @description:
 */
@Data
public class QuickMeetingDto implements Serializable {

    private Integer meetingNoType;

    private String meetingName;

    private Integer joinType;

    private String joinPassword;

    private static final long serialVersionUID = -6008081847546734698L;
}
