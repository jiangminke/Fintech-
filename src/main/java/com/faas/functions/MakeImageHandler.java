package com.faas.functions;


import com.faas.verify.core.IMakeImageHandler;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;


public class MakeImageHandler implements IMakeImageHandler<String> {
    @Setter
    private Map<String, Object> params;
    @Getter
    private String storeForVerifyStr;
    @Getter
    private String outputForViewStr;

    @Override
    public void storeForVerify(String str) {
        this.storeForVerifyStr = str;
    }

    @Override
    public void outputForView(String str) {
        this.outputForViewStr = str;
    }

    @Override
    public Map<String, Object> getBizParams() {
        return params;
    }
}
