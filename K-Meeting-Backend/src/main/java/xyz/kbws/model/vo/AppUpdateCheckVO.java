package xyz.kbws.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2026/4/6
 * @description:
 */
@Data
public class AppUpdateCheckVO implements Serializable {

    private Boolean hasUpdate;

    private Integer id;

    private String version;

    private String updateDesc;

    private Integer fileType;

    private String outerLink;

    private String downloadUrl;

    private static final long serialVersionUID = -4807050900745275316L;
}
