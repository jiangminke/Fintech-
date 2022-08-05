package com.faas.verify.core;

import java.io.Serializable;


public interface IVerifyCodeHandler<DAT extends Serializable,IPT extends Serializable>  extends IBizParams {

    /**
     * 获取验证的原始数据
     * @return
     */
    DAT getOrgData();

    /**
     * 获取交互输入的数据
     * @return
     */
    IPT getIptData();
}
