package com.automup.entity;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestResult {

    private String[] output;
    private Object result;

    private RestResult() {
    }

    public String[] getOutput() {
        return output;
    }

    public Object getResult() {
        return result;
    }

    public static RestResult create(Throwable throwable) {
        String message = throwable.getMessage() == null ? throwable.getClass().getName() : throwable.getMessage();
        return create(null, message);
    }

    public static RestResult create(Object result, String output) {
        RestResult scriptletResult = new RestResult();
        scriptletResult.result = result;
        if (StringUtils.hasLength(output)) {
            scriptletResult.output = output.split(System.lineSeparator());
        }
        
        return scriptletResult;
    }
}
