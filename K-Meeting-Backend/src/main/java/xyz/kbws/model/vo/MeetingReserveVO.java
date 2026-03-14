package xyz.kbws.model.vo;

import lombok.Data;
import xyz.kbws.model.entity.MeetingReserve;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2026/3/14
 * @description:
 */
@Data
public class MeetingReserveVO extends MeetingReserve implements Serializable {

    private String nickName;

    private static final long serialVersionUID = -3622289055555692080L;
}
