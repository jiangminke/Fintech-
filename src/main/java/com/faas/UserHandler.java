package com.faas;

import com.alibaba.fastjson.JSONObject;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.message.MessageReader;
import io.cloudevents.core.message.MessageWriter;
import io.cloudevents.http.HttpMessageFactory;
import org.eclipse.jetty.http.HttpStatus;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.utils.IOUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.faas.Application.FUNCTION_CLASS_LOADER;

public class UserHandler implements Route {
    private final Class requestType;
    private final Object functionInstance;
    private final Class functionClass;
    private final Method method;

    public UserHandler(Class requestType, Object functionInstance, Class functionClass) {
        this.requestType = requestType;
        this.functionInstance = functionInstance;
        this.functionClass = functionClass;
        // 执行用户函数
        Method method = null;
        for (Method item : functionClass.getDeclaredMethods()) {
            if ("apply".equals(item.getName())) {
                method = item;
                break;
            }
        }
        if (method == null) {
            throw new RuntimeException("method is not correct");
        }
        this.method = method;
    }

    /**
     * cloud event  输入处理
     *
     * @param httpServletRequest
     * @return
     * @throws IOException
     */
    private static MessageReader createMessageReader(HttpServletRequest httpServletRequest) throws IOException {
        Consumer<BiConsumer<String, String>> forEachHeader = processHeader -> {
            Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                processHeader.accept(name, httpServletRequest.getHeader(name));

            }
        };
        byte[] body = IOUtils.toByteArray(httpServletRequest.getInputStream());
        return HttpMessageFactory.createReader(forEachHeader, body);
    }

    /**
     * cloud event  响应处理
     *
     * @param httpServletResponse
     * @return
     * @throws IOException
     */
    private static MessageWriter createMessageWriter(HttpServletResponse httpServletResponse) throws IOException {
        return HttpMessageFactory.createWriter(
                httpServletResponse::addHeader,
                body -> {
                    try {
                        try (ServletOutputStream outputStream = httpServletResponse.getOutputStream()) {
                            if (body != null) {
                                httpServletResponse.setContentLength(body.length);
                                httpServletResponse.setStatus(HttpStatus.OK_200);
                                outputStream.write(body);
                            } else {
                                httpServletResponse.setStatus(HttpStatus.NO_CONTENT_204);
                            }
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // 修改当前线程的classloader，使得在函数jar包中的引用可以生效
            Thread.currentThread().setContextClassLoader(FUNCTION_CLASS_LOADER);
            return handleAct(request, response);
        }catch (Exception e) {
            throw e;
        }finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private Object handleAct(Request request, Response response) throws Exception {
        // 入参处理
        Object requestAct = null;
        if (Request.class.isAssignableFrom(requestType)) {
            requestAct = request;
        } else if (CloudEvent.class.isAssignableFrom(requestType)) {
            requestAct = createMessageReader(request.raw()).toEvent();
        } else if (HttpServletRequest.class.isAssignableFrom(requestType)) {
            requestAct = request.raw();
        } else if (String.class.isAssignableFrom(requestType)) {
            requestAct = request.body();
        } else if (byte[].class.isAssignableFrom(requestType)) {
            requestAct = request.bodyAsBytes();
        } else {
            requestAct = JSONObject.parseObject(request.body(), requestType);
        }

        Object resAct = method.invoke(functionInstance, requestAct);

        // 响应处理
        if (resAct == null) {
//            return "";
            response.header("Content-Type", "application/json");
            return JSONObject.toJSONString(resAct);
        } else if (resAct instanceof FaaSResponse) {
            FaaSResponse res = (FaaSResponse) resAct;
            response.status(res.getStatus());
            if (res.getHeaders() != null) {
                for (String key : res.getHeaders().keySet()) {
                    response.header(key, res.getHeaders().get(key));
                }
            }
            if (res.getBody() != null) {
                if (res.getBody() instanceof String || res.getBody() instanceof byte[]) {
                    return res.getBody();
                } else {
                    response.header("Content-Type", "application/json");
                    return JSONObject.toJSONString(res.getBody());
                }
            } else {
                return "";
            }

        } else if (resAct instanceof CloudEvent) {
            createMessageWriter(response.raw()).writeBinary((CloudEvent) resAct);
            return "";
        } else {
            response.header("Content-Type", "application/json");
            return JSONObject.toJSONString(resAct);
        }
    }
}
