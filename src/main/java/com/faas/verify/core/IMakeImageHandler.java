package com.faas.verify.core;

import java.io.Serializable;


public interface IMakeImageHandler<DAT extends Serializable> extends IBizParams {

    /**
     * 存储原始的数据用于验证交互数据
     * @param dat
     */
    void storeForVerify(DAT dat);

    /**
     * 对外输出用于显示输出
     * @param dat
     */
    void outputForView(DAT dat);
}
