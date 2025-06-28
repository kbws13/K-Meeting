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
    
    private String meetingId;
    
    private Integer messageType;
    
    private String sendUserId;
    
    private String sendUserNickName;
    
    private T messageContent;
    
    private String receiveUserId;
    
    private Long sendTime;
    
    private Long messageId;
    
    private String fileName;
    
    private Integer fileType;
    
    private Long fileSize;
    
    private static final long serialVersionUID = -2494804091690537678L;
}
