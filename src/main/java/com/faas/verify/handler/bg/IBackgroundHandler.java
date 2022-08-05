package com.faas.verify.handler.bg;

import java.awt.image.BufferedImage;


public interface IBackgroundHandler {
    /**
     * 获取背景图片
     *
     * @return
     */
    BufferedImage getBackground(Integer width,Integer height);
}
