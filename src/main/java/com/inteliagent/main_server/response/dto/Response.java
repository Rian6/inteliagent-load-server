package com.inteliagent.main_server.response.dto;

import com.inteliagent.main_server.response.interfaces.IDefaultResponse;
import com.inteliagent.main_server.util.JsonUtil;

public class Response implements IDefaultResponse {
    private int status;
    private String message;
    private Object content;

    public Response() {
    }

    public Response(String content) {
        this.content = content;
    }

    public Response setContent(Object content) {
        this.content = content;
        return this;
    }

    public Object getContent() {
        return content;
    }

    public int getStatus() {
        return status;
    }

    public Response setStatus(int status) {
        this.status = status;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public Response setMessage(String message) {
        this.message = message;
        return this;
    }

    @Override
    public String build(){
        return JsonUtil.toJson(this);
    }
}
