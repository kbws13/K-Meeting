package xyz.kbws.model.dto.meeting;

import lombok.Data;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2026/3/7
 * @description:
 */
@Data
public class JoinDto implements Serializable {

    private Boolean videoOpen;
    
    private static final long serialVersionUID = -2555509102276582299L;
}
