package com.atguigu.gulimall.order.service.impl;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.order.dao.OrderItemDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@RabbitListener(queues = {"hello-java-queue"})
@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * queues:声明需要监听的所有队列
     *org.springframework.amqp.core.Message
     *  1\Message message 原生消息详细信息 头加体
     *  2、T<发送消息的类型> OrderReturnReasonEntity context
     *  3、Channel channel:当前传输数据的通道
     *
     *
     *  Queue：可以很多的人都来监听 只要收到消息 队列删除消息 而且只能有一个收到此消息
     *  场景
     *      1）订单服务启动多个 同一个消息 只能有一个客户收到
     *      2）、只有一个消息完全处理完 方法运行结束 我们就可以接受到下一个消息
     *
     */
    //@RabbitListener(queues = {"hello-java-queue"})
    @RabbitHandler
    public void recieveMessage(Message message, OrderReturnReasonEntity context,
                               Channel channel) throws InterruptedException {
        //{"id":null,"memberId":null,"orderSn":null,"couponId":null,"createTime":1644999422947,"memberUser
        System.out.println("接受到信息。。。内容"+message+"-->内容"+context);
        byte[] body = message.getBody();
        //消息头属性信息
        MessageProperties messageProperties = message.getMessageProperties();
        //Thread.sleep(3000);
        System.out.println("消息处理完成==》"+context);

    }

    @RabbitHandler
    public void recieveMessage2( OrderEntity context
                              ) throws InterruptedException {

        System.out.println("消息处理完成==》"+context);

    }
}