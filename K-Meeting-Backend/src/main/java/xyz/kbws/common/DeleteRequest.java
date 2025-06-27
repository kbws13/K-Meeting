package xyz.kbws.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2025/6/27
 * @description:
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Integer id;

    private static final long serialVersionUID = 8074743987751310835L;
}
