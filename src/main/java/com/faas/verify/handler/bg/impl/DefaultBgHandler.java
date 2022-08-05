package com.faas.verify.handler.bg.impl;

import com.faas.verify.utils.PropertyUtil;

import java.awt.image.BufferedImage;


public class DefaultBgHandler extends AbsBaseBgHandler {
    private String handlerName;

    public DefaultBgHandler(String handlerName) {
        this.handlerName = handlerName;
    }

    @Override
    public BufferedImage getBackground(Integer width,Integer height) {
        return dealFitSize(width,height, PropertyUtil.getRandom(this.handlerName));

    }
}
