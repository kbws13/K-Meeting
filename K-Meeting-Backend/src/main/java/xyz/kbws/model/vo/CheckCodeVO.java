package xyz.kbws.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2025/6/28
 * @description:
 */
@Data
public class CheckCodeVO implements Serializable {

    private String checkCodeKey;

    private String checkCode;

    private static final long serialVersionUID = 4081290697544567030L;
}
