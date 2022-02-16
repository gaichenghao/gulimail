package com.atguigu.gulimall.order;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.UUID;

@SpringBootTest
@Slf4j
@RunWith(SpringRunner.class)
public class GulimallOrderApplicationTests {

    @Autowired
    AmqpAdmin amqpAdmin;

    @Autowired
    RabbitTemplate rabbitTemplate;
    /**
     * 1\如何创建Exchange(hello-java-exchange) Queue Binding
     * 1）、使用AmqpAdmin进行创建
     * 2、如何收发消息
     *
     */
    @Test
    public void createExchange() {
        //AmqpAdmin
        //Exchange
        /**
         *   public DirectExchange(String name, boolean durable, boolean autoDelete, Map<String, Object> arguments)
         */
        DirectExchange directExchange=new DirectExchange("hello-java-exchange",true,false);
        amqpAdmin.declareExchange(directExchange);
        log.info("Exchange[{}]创建成功","hello-java-exchange");
    }

    @Test
    public void createQueue(){
        /**
         *    public Queue(String name, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments)
         */
        Queue queue=new Queue("hello-java-queue",true,false,false);
        amqpAdmin.declareQueue(queue);
        log.info("Queue[{}]创建成功","hello-java-queue");
    }

    @Test
    public void createBinding(){
        //(String destination,[目的地]
        // Binding.DestinationType destinationType,【目的地类型】
        // String exchange, 【交换机】
        // String routingKey,【路由器】
        // Map<String, Object> arguments)【自定义参数】
        //将exchange指定的交换机 和destination目的地进行绑定 使用routingkey作为指定的路由键
        Binding binding=new Binding("hello-java-queue",
                Binding.DestinationType.QUEUE,
                "hello-java-exchange",
                "hello.java",null);
        amqpAdmin.declareBinding(binding);
        log.info("Binding[{}]创建成功","hello-java-binding");
    }

    @Test
    public void sendMessageTest(){
        //1\发送消息 如果发送的消息是个对象 我们会使用序列化机制 将对象写出去 对象必须实现Serializable
        String msg="hello world!";


        //2\发送的对象要是一个json


        for(int i=0;i<10;i++){
            if(i%2==0){
                OrderReturnReasonEntity order=new OrderReturnReasonEntity();
                order.setId(1L);
                order.setCreateTime(new Date());
                order.setName("AAAA-->"+i);
                rabbitTemplate.convertAndSend("hello-java-exchange","hello.java",order);

            }else {
                OrderEntity entity=new OrderEntity();
                entity.setOrderSn(UUID.randomUUID().toString());
                rabbitTemplate.convertAndSend("hello-java-exchange","hello.java",entity);
            }
            log.info("消息发送完成{}");
        }

    }

}
