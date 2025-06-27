package xyz.kbws.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @author kbws
 * @date 2025/6/27
 * @description:
 */
@Data
public class BaseResponse<T> implements Serializable {

    private Integer code;

    private T data;

    private String message;

    private static final long serialVersionUID = 7092500290722078455L;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
