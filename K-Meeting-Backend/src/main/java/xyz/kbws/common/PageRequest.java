package xyz.kbws.common;

import lombok.Data;
import xyz.kbws.constant.CommonConstant;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2025/6/27
 * @description:
 */
@Data
public class PageRequest implements Serializable {

    /**
     * 当前页号
     */
    private long current = 1;

    /**
     * 页面大小
     */
    private long pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认升序）
     */
    private String sortOrder = CommonConstant.SORT_ORDER_ASC;

    private static final long serialVersionUID = 3805087817747573771L;
}
