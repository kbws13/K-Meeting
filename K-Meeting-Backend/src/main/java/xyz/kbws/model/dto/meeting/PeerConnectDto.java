package xyz.kbws.model.dto.meeting;

import lombok.Data;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2026/3/8
 * @description:
 */
@Data
public class PeerConnectDto implements Serializable {
    
    private String token;
    
    private String sendUserId;
    
    private String receiveUserId;
    
    private String signalType;
    
    private String signalData;
    
    private static final long serialVersionUID = 823333499624870160L;
}
