package xyz.kbws.model.entity;

import lombok.Data;

/**
 * @author kbws
 * @date 2026/4/2
 * @description:
 */
@Data
public class SystemSetting {
    private Integer maxImageSize = 2;
    private Integer maxVideoSize = 5;
    private Integer maxFileSize = 5;
}
