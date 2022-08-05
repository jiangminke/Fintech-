package com.faas.verify.handler;

import com.faas.verify.verifier.PointClickVerifier;
import com.faas.verify.utils.ConstUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class PointClickHander extends ImgHttpBaseHandler {
    public static final PointClickHander INSTANCE = new PointClickHander();


    private final static String VERIFY_KEY_NAME = "人之初,性本善,性相近,习相远,苟不教,性乃迁,教之道,贵以专,昔孟母,择邻处,子不学,断机杼,窦燕山,有义方,教五子,名俱扬,养不教,父之过,教不严,师之惰,子不学,非所宜,幼不学,老何为,玉不琢,不成器,人不学,不知义,为人子,方少时,亲师友,习礼仪,香九龄,能温席,孝于亲,所当执";
    private PointClickVerifier pointClickVerifier;

    public PointClickHander() {
        this.pointClickVerifier = new PointClickVerifier(Arrays.stream(VERIFY_KEY_NAME.split(",")).collect(Collectors.toList()));
        this.setCaptchaVerifier(this.pointClickVerifier);
    }

    public void setKeyName(List<String> keyNames) {
        this.pointClickVerifier.setKeyNames(keyNames);
    }

    @Override
    public void initBuildParams() {
        if(!childParams.containsKey(ConstUtil.KEY_POINT_DIS_RADIUS)){
            childParams.put(ConstUtil.KEY_POINT_DIS_RADIUS,ConstUtil.DEF_POINT_DIS_RADIUS);
        }
    }

    @Override
    public String getHandlerName() {
        return ConstUtil.CONS_POINT_CLICK_HANDLER;
    }
}
