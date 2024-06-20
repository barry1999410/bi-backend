package com.yupi.springbootinit.model.dto.smscode;

import lombok.Data;

import java.io.Serializable;

@Data
public class SmsCodeRequest implements Serializable {

    private String code;
    private String phone;
}
