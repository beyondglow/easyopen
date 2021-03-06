package com.gitee.easyopen.exception;

import com.gitee.easyopen.message.Error;
import com.gitee.easyopen.message.Errors;

/**
 * @author tanghc
 */
public class ApiException extends RuntimeException {
    private static final long serialVersionUID = 16789476595630713L;
    private static final String ERROR_CODE = Errors.SYS_ERROR.getCode();

    private String code = ERROR_CODE;
    private Object data;

    public ApiException(String msg) {
        super(msg);
    }

    public ApiException(Exception e) {
        super(e);
    }

    public ApiException(Error<String> error) {
        this(error.getMsg());
        this.code = error.getCode();
    }

    public ApiException(String msg, String code) {
        super(msg);
        this.code = code;
    }

    public ApiException(String msg, String code, Object data) {
        super(msg);
        this.code = code;
        this.data = data;
    }

    public ApiException(Error<String> error, Object data) {
        this(error);
        this.data = data;
    }

    public String getCode() {
        return code;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
    
    /**
     * 获取RuntimeException异常，在做dubbo服务时可以用到，因为dubbo处理自定义异常有问题。
     */
    public RuntimeException getRuntimeException() {
        return new RuntimeException(this.getMessage());
    }

}
