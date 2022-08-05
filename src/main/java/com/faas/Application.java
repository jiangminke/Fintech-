package com.faas;

import cn.hutool.http.HttpUtil;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static spark.Spark.*;

public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    private static final String DEFAULT_HANDLER = "com.faas.functions.Function";

    // classloader 在跨线程传参的时候有些问题，对象id相同，并且内部属性也没区别，但是在子线程中不能加载函数jar包中引用的类，
    // 具体逻辑还不理解，暂时改用静态共享的方式，将函数的claasloader全局共享，并在userhandler中调用
    public static ClassLoader FUNCTION_CLASS_LOADER;

    static {
        String functionClassPath = System.getenv("FAAS_CLASS_PATH");
        ClassLoader cls = Application.class.getClassLoader();
        if (!StringUtil.isBlank(functionClassPath)) {
            FUNCTION_CLASS_LOADER = new URLClassLoader(classpathToUrls(functionClassPath), cls);
        } else {
            FUNCTION_CLASS_LOADER = cls;
        }
    }

    public static void main(String[] arg) throws Exception {

        URLClassLoader urlClassLoader = null;
        try {

            // 端口
            String port = System.getenv("FAAS_PORT");
            int portAct;
            if (StringUtil.isBlank(port)) {
                portAct = 8080;
            } else {
                portAct = Integer.parseInt(port);
            }
            port(portAct);

            // 默认返回的路径
            regisIgnoredHandler();

            // 用户函数路径
            regisUserHandler();

            // 心跳检测
            reportStatus();
        } catch (Exception e) {
            logger.error("函数启动异常", e);
            throw (e);
        } finally {
            if (urlClassLoader != null) {
                urlClassLoader.close();
            }
        }
    }

    private static void reportStatus() {
        String statusReportUrl = FaaSUtils.getEnv("FAAS_STATUS_REPORT_URL", null);
        String faasHealthCheckInterval = FaaSUtils.getEnv("FAAS_HEALTH_CHECK_INTERVAL", "5000");
        if (statusReportUrl == null) {
            return;
        }
        awaitInitialization();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    HttpUtil.get(statusReportUrl, 1000);
                } catch (Exception e) {
                    logger.error("上报状态异常", e);
                }

            }
        }, 0, Integer.parseInt(faasHealthCheckInterval));
    }

    static URL[] classpathToUrls(String classpath) {
        String[] components = classpath.split(File.pathSeparator);
        List<URL> urls = new ArrayList<>();
        for (String component : components) {
            if (component.endsWith(File.separator + "*")) {
                urls.addAll(jarsIn(component.substring(0, component.length() - 2)));
            } else {
                Path path = Paths.get(component);
                try {
                    urls.add(path.toUri().toURL());
                } catch (MalformedURLException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        return urls.toArray(new URL[0]);
    }

    private static List<URL> jarsIn(String dir) {
        Path path = Paths.get(dir);
        if (!Files.isDirectory(path)) {
            return Collections.emptyList();
        }
        Stream<Path> stream = null;
        try {
            stream = Files.list(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
        return stream
                .filter(p -> p.getFileName().toString().endsWith(".jar"))
                .map(p -> {
                    try {
                        return p.toUri().toURL();
                    } catch (MalformedURLException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .collect(toList());
    }

    // 以下路径默认返回
    private static void regisIgnoredHandler() throws Exception {
        staticFileLocation("/web");

        get("/favicon.ico", IgnoredHandler.INSTANCE);
        get("/healthz", IgnoredHandler.INSTANCE);
        get("/healthz/ready", IgnoredHandler.INSTANCE);

    }

    /**
     * 注册用户函数
     *
     * @throws Exception
     */
    private static void regisUserHandler() throws Exception {
        String handler = System.getenv("FAAS_HANDLER");
        if (StringUtil.isBlank(handler)) {
            handler = DEFAULT_HANDLER;
        }
        Class clazz = FUNCTION_CLASS_LOADER.loadClass(handler);
        Type[] types = ((ParameterizedTypeImpl) clazz.getGenericInterfaces()[0]).getActualTypeArguments();
        if (types.length != 2) {
            throw new RuntimeException("handler is not correct");
        }
        final Class requestType = FUNCTION_CLASS_LOADER.loadClass(types[0].getTypeName());

        Object inst = clazz.getDeclaredConstructor().newInstance();
        if (!(inst instanceof Function)) {
            throw new RuntimeException("handler is not correct");
        }

        UserHandler userHandler = new UserHandler(requestType, inst, clazz);
        post("/*", userHandler);
        get("/*", userHandler);
        put("/*", userHandler);
        delete("/*", userHandler);
    }
}


/**
 * 需要忽略的处理函数
 */
class IgnoredHandler implements Route {
    public static final IgnoredHandler INSTANCE = new IgnoredHandler();

    @Override
    public Object handle(Request request, Response response) throws Exception {
        return "ok";
    }
}