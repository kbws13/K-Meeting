package xyz.kbws.model.dto.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageSendDto<T> implements Serializable {

    private Integer messageSend2Type;

    private Integer meetingId;

    private Integer messageType;

    private Integer sendUserId;

    private String sendUserNickName;

    private T messageContent;

    private Integer receiveUserId;

    private Long sendTime;

    private Integer messageId;

    private String fileName;

    private Integer fileType;

    private Long fileSize;
    
    private Integer status;

    private static final long serialVersionUID = -2494804091690537678L;
}
