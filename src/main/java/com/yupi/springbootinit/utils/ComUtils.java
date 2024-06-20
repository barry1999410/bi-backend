package com.yupi.springbootinit.utils;

import java.util.Random;

public class ComUtils {


    private static final Random random = new Random();

    /**
     * 生成验证码
     * @return
     */
    public static String generateVerificationCode() {
        int verificationCode = 1000 + random.nextInt(9000); // 生成4位数随机验证码
        return String.valueOf(verificationCode);
    }



}
