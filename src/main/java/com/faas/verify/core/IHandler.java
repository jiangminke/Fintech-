package com.faas.verify.core;


public interface IHandler<OUT, VER, MAK> {
    boolean verifyCode(VER bizVerifyHandler);

    OUT buildCaptcha(MAK bizVerifyHandler);
}
