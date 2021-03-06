package com.atguigu.gulimall.order.config;


import com.atguigu.gulimall.order.entity.OrderEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MyMQConfig {

    @RabbitListener(queues = "order.release.order.queue")
    public void listener(OrderEntity entity, Channel channel, Message message) throws IOException {
        System.out.println("收到过期的订单信息 准备关闭订单"+entity.getOrderSn());
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

    }



    //@bean Binding Queue Exchange

    /**
     * 容器中 @bean Binding Queue Exchange 都会自动创建（rabbitmq没有的情况）
     * rabbitmq 只要有 属性发生变化 也不会覆盖
     */
    @Bean
    public Queue orderDelayQueue(){
        Map<String, Object> arguments=new HashMap<>();
        arguments.put("x-dead-letter-exchange","order-event-exchange");
        arguments.put("x-dead-letter-routing-key","order.release.order");
        arguments.put("x-message-ttl",60000);

        //String name, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments)
        Queue queue = new Queue("order.delay.queue", true, false, false,arguments);


        return queue;

    }

    @Bean
    public  Queue orderReleaseOrderQueue(){

        Queue queue = new Queue("order.release.order.queue", true, false, false);


        return queue;

    }

    @Bean
    public Exchange orderEventExchange(){
        //String name, boolean durable, boolean autoDelete, Map<String, Object> arguments
        return new TopicExchange("order-event-exchange",true,false);

    }

    @Bean
    public Binding orderCreateOrderBinding(){
        //String destination, Binding.DestinationType destinationType, String exchange, String routingKey, Map<String, Object> arguments
        return  new Binding("order.delay.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
               "order.create.order",null );

    }

    @Bean
    public Binding orderReleaseOrderBinding(){
        //String destination, Binding.DestinationType destinationType, String exchange, String routingKey, Map<String, Object> arguments
        return  new Binding("order.release.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.order",null );


    }
}
