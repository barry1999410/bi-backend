package com.yupi.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.yupi.springbootinit.constant.BiConstant;

public class NqInitMain {

    public static void main(String[] args) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            String exchangeName = BiConstant.BI_EXCHANGE_NAME;
            channel.exchangeDeclare(BiConstant.BI_EXCHANGE_NAME,"direct");
            //创建队列
            String queueName = BiConstant.BI_QUEUE_NAME;
            String routingKey = BiConstant.BI_ROUTING_KEY;
            channel.queueDeclare(queueName,true,false,false,null);
            channel.queueBind(queueName,exchangeName,routingKey);
        }catch (Exception e){

        }
    }

}
