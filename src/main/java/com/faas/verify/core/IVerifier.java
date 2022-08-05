package com.faas.verify.core;

import java.io.Serializable;
import java.util.Map;


public interface IVerifier<B extends Serializable, T extends Serializable> {
    /**
     * 验证
     *
     * @param toBeVerifyEntity
     * @return
     */
    boolean verifyCode(B toBeVerifyEntity);

    /**
     * 生产验证码
     *
     * @param buildParams
     * @return
     */
    T[] buildCaptcha(Map<String, Object> buildParams);

}
