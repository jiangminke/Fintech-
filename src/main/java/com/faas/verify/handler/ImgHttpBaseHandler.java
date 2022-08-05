package com.faas.verify.handler;

import com.alibaba.fastjson.JSON;
import com.faas.verify.core.IMakeImageHandler;
import com.faas.verify.core.IVerifyCodeHandler;
import com.faas.verify.core.IHandler;
import com.faas.verify.core.IVerifier;
import com.faas.verify.entity.CaptchaDataEntity;
import com.faas.verify.entity.VerifyEntity;
import com.faas.verify.entity.ImgInteractionEntity;
import com.faas.verify.handler.bg.IBackgroundHandler;
import com.faas.verify.handler.bg.impl.DefaultBgHandler;
import com.faas.verify.utils.Assert;
import com.faas.verify.utils.ConstUtil;
import com.faas.verify.utils.ImageUtil;
import com.faas.verify.utils.SecurityUtil;
import lombok.Setter;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class ImgHttpBaseHandler  implements IHandler<String, IVerifyCodeHandler<String, String>, IMakeImageHandler> {
    protected Map<String, Object> childParams = new HashMap<>();

    public void setChildParams(String key, Object val) {
        childParams.put(key, val);
    }

    public abstract void initBuildParams();
    @Setter
    private IBackgroundHandler backgroundHandler = new DefaultBgHandler(getHandlerName());
    private IVerifier<VerifyEntity, CaptchaDataEntity<ImgInteractionEntity>> captchaVerifier = null;

    public abstract String getHandlerName();

    public void setCaptchaVerifier(IVerifier<VerifyEntity, CaptchaDataEntity<ImgInteractionEntity>> captchaVerifier) {
        this.captchaVerifier = captchaVerifier;
    }


    @Override
    public boolean verifyCode(IVerifyCodeHandler<String, String> bizVerifyHandler) {
        return this.captchaVerifier.verifyCode(convertForInnerVerify(bizVerifyHandler.getOrgData(), bizVerifyHandler.getIptData()));
    }

    @Override
    public String buildCaptcha(IMakeImageHandler makeImageHandler) {
        Map<String, Object> stringObjectMap = makeImageHandler.getBizParams();
        beforeBuildParams(stringObjectMap);
        CaptchaDataEntity<ImgInteractionEntity>[] captchaArr = this.captchaVerifier.buildCaptcha(stringObjectMap);
        afterBuildParams(stringObjectMap);
        String ret = convertForView(getHandlerName(), captchaArr, stringObjectMap);
        makeImageHandler.outputForView(ret);
        makeImageHandler.storeForVerify(convertForStoreVerify(captchaArr,stringObjectMap ));
        return ret;
    }

    protected String convertForView(String buildFactoryName, CaptchaDataEntity<ImgInteractionEntity>[] captchaArr, Map<String, Object> buildParams) {
        Map<String,Object> retAll = new HashMap<>();
        retAll.put("factory",buildFactoryName);
        retAll.put("params",JSON.parseObject(JSON.toJSONString(buildParams)));
        List<Map<String,Object>> ret = new ArrayList<>();
        Map<String,Object> objectMap;
        for(CaptchaDataEntity<ImgInteractionEntity> item:captchaArr){
            objectMap = new HashMap<>();
            objectMap.put("handler",getHandlerName());
            objectMap.put("type",item.getCaptchaType());
            objectMap.put("img", ImageUtil.imgBase64(item.getOriginalObj().getImgByte()));
            ret.add(objectMap);
        }
        retAll.put("data",ret);
        return JSON.toJSONString(retAll);
    }

    protected VerifyEntity convertForInnerVerify(String orgData, String iptData) {
        orgData = SecurityUtil.decode(orgData)[0];
        Assert.notNull(orgData,"orgData is null");
        Assert.hasText(orgData, "orgData is empty str");
        Assert.hasText(iptData, "iptData is empty str");
        VerifyEntity verifyEntity = new VerifyEntity();
        verifyEntity.setOriginalByte(orgData.getBytes());
        verifyEntity.setVerifyByte(iptData.getBytes());
        return verifyEntity;
    }


    protected String convertForStoreVerify(CaptchaDataEntity<ImgInteractionEntity>[] captchaArr, Map<String, Object> buildParams) {
        for(CaptchaDataEntity<ImgInteractionEntity> item:captchaArr){
            if(CaptchaDataEntity.CaptchaType.PRIMARY_IMG.equals(item.getCaptchaType())){
                String data = JSON.toJSONString(item.getOriginalObj().getImgPositionEntities());
                String parms = JSON.toJSONString(buildParams);
                String ret = SecurityUtil.encode(data,parms);
                return ret;
            }
        }
        return null;
    }


    protected void beforeBuildParams(Map<String, Object> bizParams) {
        if (bizParams == null) {
            bizParams = new HashMap<>();
        }
        this.initBuildParams();
        for (Map.Entry<String, Object> entry : this.childParams.entrySet()) {
            if (!bizParams.containsKey(entry.getKey()))
                bizParams.put(entry.getKey(), entry.getValue());
        }
        bizParams.put("BUILD_TYPE", getHandlerName());
        if(!bizParams.containsKey(ConstUtil.CON_BG_HANDLER)) {
            bizParams.put(ConstUtil.CON_BG_HANDLER, backgroundHandler);
        }

        if(!bizParams.containsKey(ConstUtil.KEY_IMG_BG_WIDTH)){
            bizParams.put(ConstUtil.KEY_IMG_BG_WIDTH, ConstUtil.DEF_LONG_IMG_BG_WIDTH);
        }
        if(!bizParams.containsKey(ConstUtil.KEY_IMG_BG_HEIGHT)){
            bizParams.put(ConstUtil.KEY_IMG_BG_HEIGHT, ConstUtil.DEF_LONG_IMG_BG_HEIGHT);
        }
    }


    protected void afterBuildParams(Map<String, Object> bizParams) {
        if(bizParams.containsKey(ConstUtil.CON_BG_HANDLER)) {
            bizParams.remove(ConstUtil.CON_BG_HANDLER);
        }
    }
}
