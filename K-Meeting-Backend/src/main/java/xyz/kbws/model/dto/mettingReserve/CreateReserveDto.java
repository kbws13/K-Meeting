package xyz.kbws.model.dto.mettingReserve;

import lombok.Data;
import xyz.kbws.model.entity.MeetingReserve;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2026/3/14
 * @description:
 */
@Data
public class CreateReserveDto extends MeetingReserve implements Serializable {

    private String inviteUserIds;

    private static final long serialVersionUID = 6138534699806347311L;
}
