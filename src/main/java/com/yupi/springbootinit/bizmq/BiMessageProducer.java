package com.yupi.springbootinit.bizmq;


import com.yupi.springbootinit.constant.BiConstant;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class BiMessageProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(BiConstant.BI_EXCHANGE_NAME, BiConstant.BI_ROUTING_KEY, message);
    }


}
