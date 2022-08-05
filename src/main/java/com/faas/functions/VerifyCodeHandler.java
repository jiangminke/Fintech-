package com.faas.functions;


import com.faas.verify.core.IVerifyCodeHandler;
import lombok.Setter;

import java.util.Map;


public class VerifyCodeHandler implements IVerifyCodeHandler<String, String> {
    @Setter
    private String orgData;
    @Setter
    private String iptData;

    @Override
    public String getOrgData() {
        return orgData;
    }

    @Override
    public String getIptData() {
        return iptData;
    }

    @Override
    public Map<String, Object> getBizParams() {
        return null;
    }
}
