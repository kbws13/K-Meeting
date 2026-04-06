package xyz.kbws.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 应用更新表
 * @TableName appUpdate
 */
@TableName(value ="appUpdate")
@Data
public class AppUpdate {
    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 版本号
     */
    private String version;

    /**
     * 版本更新说明
     */
    private String updateDesc;

    /**
     * 
     */
    private Integer status;

    /**
     * 
     */
    private String grayscaleId;

    /**
     * 文件类型
     */
    private Integer fileType;

    /**
     * 外链
     */
    private String outerLink;

    /**
     * 创建时间
     */
    private Date createTime;
}