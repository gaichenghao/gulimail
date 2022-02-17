package com.atguigu.gulimall.order.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class MyRabbitConfig {


    @Autowired
    RabbitTemplate rabbitTemplate;

    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    /**
     *  定制RabbitTemplate
     *  1\服务受到消息就回调
     *      1、spring.rabbitmq.publisher-confirms=true
     *      2\设置确认回调
     *  2\消息正确抵达队列进行回调
     *      1、    publisher-returns: true
     *     #只要抵达队列 一异步发送优先回调我们这个returnconfirm
     *     template:
     *       mandatory: true
     *       2、设置确认回调 returncallback
     *   3、消费端确认（保证每个消息被正确消费 此时才可以broker删除这个消息）
     *      1、默认是自动确认的 只要消息接受到 客户端就会自动确认 服务端就会移除这个消息
     *          问题 ：
     *              我们收到很多消息 自动回复给服务器ack 只有一个消息处理成功 宕机了 发生消息丢失：
     *              消费者手动确认。只要没有明确告诉mq 货物被签收 没有ack 消息就一直是unacked状态· 即使 Consumer宕机 消息不会丢失
     *              会重新变为ready 下一次 有新的Consumer链接进来就发给他
     *       2、如何签收
     *       channel.basicAck() 签收 业务成功完成 就应该签收
     *       channel.basicNack() 拒签 业务失败 拒签
     *
     */
    @PostConstruct //MyRabbitConfig对象创建完以后 执行这个方法
    public void initRabbitTemplate(){
        //设置确认回调
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /**
             *
             * @param correlationData 当前消息的唯一关联数据（这个是消息的唯一id）
             * @param b 消息是否成功收到
             * @param s 失败的原因
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean b, String s) {
                System.out.println("comfirm....correlationData【"+correlationData+"】-->ack【"+b+"】===>cause["+s+"]");
            }
        });

        //设置消息抵达队列的确认回调
        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            /**
             * 只要消息没有投递给指定的队列 就触发这个失败回调
             * @param message 投递失败的消息详细信息
             * @param i 回复的状态码
             * @param s 回复的文本内容
             * @param s1 当时这个消息发给哪个交换机
             * @param s2 当时这个消息用哪个路由键
             */
            @Override
            public void returnedMessage(Message message, int i, String s, String s1, String s2) {
                System.out.println("fail message["+message+"]-->replycode["+i+"]-->exchange["+s1+"]-=-->routingKey["+s2+"]");
            }
        });

    }
}
