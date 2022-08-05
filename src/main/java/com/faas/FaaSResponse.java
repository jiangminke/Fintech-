package com.faas;

import java.util.Map;

public class FaaSResponse {
    private int status = 200;
    private Map<String, String> headers;
    private Object body = "";

    public FaaSResponse(int status) {
        this.status = status;
    }

    public FaaSResponse(int status, Map<String, String> headers) {
        this.status = status;
        this.headers = headers;
    }

    public FaaSResponse(int status, Map<String, String> headers, Object body) {
        this.status = status;
        this.headers = headers;
        this.body = body;
    }

    public FaaSResponse(Object body) {
        this.body = body;
    }

    public FaaSResponse(Map<String, String> headers, Object body) {
        this.headers = headers;
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }
}
