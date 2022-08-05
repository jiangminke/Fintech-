package com.faas.verify.entity;


import lombok.Data;

import java.io.Serializable;


@Data
public class VerifyEntity implements Serializable {
    private byte[] originalByte; //原始的数据
    private byte[] verifyByte; //交互的数据
}
