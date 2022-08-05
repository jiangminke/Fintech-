package com.faas;

import cn.hutool.http.HttpRequest;
import com.google.common.net.HttpHeaders;
import spark.utils.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FaaSUtils {

    public static String getEnv(String name, String defaultValue) {
        String result = System.getenv(name);
        if (!StringUtils.isBlank(result)) {
            return result;
        }
        return defaultValue;
    }

    public static void setTraceHeader(HttpServletRequest req, HttpRequest nextReq){

        Map<String, String> traceHeader = new HashMap<>();
        traceHeader.put("X-B3-BusinessId", req.getHeader("x-b3-businessid"));
        traceHeader.put("X-B3-TraceId", req.getHeader("x-b3-traceid"));
        traceHeader.put("X-B3-SpanId", UUID.randomUUID().toString().replaceAll("-","").substring(0,16));
        traceHeader.put("X-B3-ParentSpanId", req.getHeader("x-b3-spanid"));
        traceHeader.put("X-B3-Timestamp", String.valueOf(new Date().getTime()));
        traceHeader.put("X-B3-Sampled", req.getHeader("x-b3-sampled"));
        traceHeader.put("X-B3-Debug", req.getHeader("x-b3-debug"));
        nextReq.headerMap(traceHeader, true);
    }

}
