package com.faas.functions;

import com.alibaba.fastjson.JSON;
import com.faas.Bucket;
import com.faas.FaaSResponse;
import com.faas.SliderWindowRateLimiter;
import com.faas.verify.entity.UserRecord;
import com.faas.verify.handler.ImgHttpBaseHandler;
import com.faas.verify.utils.Assert;
import com.faas.verify.utils.ConstUtil;
import com.faas.verify.utils.SecurityUtil;
import lombok.val;
import org.apache.poi.hssf.record.chart.ObjectLinkRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Session;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class Function implements java.util.function.Function<Request, Object> {
    private static final Logger logger = LoggerFactory.getLogger(Function.class);
    private static final String METHOD_CAPTCHA = "/captcha";
    private static final String METHOD_VERIFY = "/verifyCode";
    public static final String CAPTCHA_KEY = "captchaKey";
    public static final String PARAMS_HANDLER_TYPE = "HANDLER_TYPE";

    //这里的核心线程数需要合理设置
    public static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10);


    public static final Map<String, SliderWindowRateLimiter> windowMap = new HashMap<>();
    public static final Bucket bucket = new Bucket(10, 1);

    static {
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            bucket.put();
        }, 0, 1, TimeUnit.SECONDS);
    }

    public Object apply(Request request) {
        //限流 layer
        if(!bucket.get()) {
            //flow limit
            FaaSResponse faaSResponse =
                    new FaaSResponse(302);
            faaSResponse.setBody("使用人数太多");
            return faaSResponse;
        }

        String uri = request.uri();
        HttpServletRequest httpRequest = request.raw();
        HttpSession session = httpRequest.getSession();
        String sessionId = session.getId();

        SliderWindowRateLimiter window = null;
        if(!windowMap.containsKey(sessionId)) {
            window = new SliderWindowRateLimiter(30, 5);
            windowMap.put(sessionId, window);
            scheduledExecutorService.scheduleAtFixedRate(window, 1000, 1000, TimeUnit.MILLISECONDS);
        }
        window = windowMap.get(sessionId);

        if (METHOD_CAPTCHA.equals(uri)) {
            System.out.println("log-captcha");
            return captcha(request);
        } else if (METHOD_VERIFY.equals(uri)) {
            boolean res = (boolean) verify(request);
            System.out.println("log-verify");
            if(window.isOverLimit()) {
                FaaSResponse faaSResponse =
                        new FaaSResponse(301);
                faaSResponse.setBody("请稍等一会重试");
                return faaSResponse;
            } else {
                if(!res) {
                    window.visit();
                }
                return res;
            }

        }
        return null;
    }


    public Object captcha(Request request) {
        String handler = "puzzle".equalsIgnoreCase(request.queryParams("handler")) ? ConstUtil.CONS_PUZZLE_HANDLER : ConstUtil.CONS_POINT_CLICK_HANDLER;
        MakeImageHandler makeImageHandler = new MakeImageHandler();
        Map<String, Object> params = new HashMap<>();
        params.put(PARAMS_HANDLER_TYPE, handler);
        makeImageHandler.setParams(params);
        HandlersEnum.getHandler(handler).buildCaptcha(makeImageHandler);
        HttpSession session = request.raw().getSession();
        session.setAttribute(CAPTCHA_KEY, makeImageHandler.getStoreForVerifyStr());
        return makeImageHandler.getOutputForViewStr();
    }

    public Object verify(Request request) {
        String iptData = request.body();
        VerifyCodeHandler bizVerifyHandler = new VerifyCodeHandler();
        HttpSession session = request.raw().getSession();
        bizVerifyHandler.setOrgData(session.getAttribute(CAPTCHA_KEY).toString());
        bizVerifyHandler.setIptData(iptData);

        String[] dataDecode = SecurityUtil.decode(new String(bizVerifyHandler.getOrgData()));
        Assert.notNull(dataDecode, "dataDecode is err");
        Map<String, Object> buildParams = JSON.parseObject(dataDecode[1]);
        String handlerTypeStr = (String) buildParams.get(PARAMS_HANDLER_TYPE);
        Assert.hasText(handlerTypeStr, "handlerType is empty!");
        ImgHttpBaseHandler handler = HandlersEnum.getHandler(handlerTypeStr);
        Assert.notNull(handler, "handlerType is err");

        return handler.verifyCode(bizVerifyHandler);
    }


}
