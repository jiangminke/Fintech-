package com.faas.verify.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;


@Data
public class CaptchaDataEntity<T extends Serializable> implements Serializable {
    private CaptchaType captchaType;//验证码类型
    private T originalObj; //原始的数据
    private Map<String,Object> params; //辅助参数

    public enum CaptchaType{
        /**
         * 主图片
         */
        PRIMARY_IMG(0),
        /**
         * 辅助图片
         */
        SECONDARY_IMG(1);
        private  int index;
        CaptchaType(int idx){
            this.index = idx;
        }
    }
}
