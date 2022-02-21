package com.atguigu.gulimall.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;


/**
 * 使用rabbitmq
 * 1、引用amqp场景 RabbitAutoConfiguration就会自动生效
 *
 * 2、给容器中自动配置了
 *  rabbitTemplate AmqpAdmin rabbitMessagingTemplate CachingConnectionFactory
 *  所有的属性都是 spring-rabbitmq
 *  @ConfigurationProperties(
 *     prefix = "spring.rabbitmq"
 * )
 * public class RabbitProperties
 * 3\给配置文件中配置 spring.rabbitmq 信息
 * 4、@EnableRabbit :@EnableXxxxx：开启功能
 * 5\监听消息 使用@RabbitListener 必须有@EnableRabbit
 * @RabbitListener 类+方法上 (监听哪些队列即可)
 * @RabbitHandler 方法上 （重载区分不同的消息）
 *
 * 本地事务失效问题
 *     //本地事务 在分布式系统 只能控制住自己的回滚 控制不了其他服务的回滚
 *     //分布式事务 最大原因 网络问题 +分布式机器
 *
 *  解决方案：使用代理对象来调用事务方法
 *  1）引入aop_starter 引入le aspectj
 *  2）@EnableAspectJAutoProxy(exposeProxy = true)：开启aspectj动态代理 功能。以后所有的动态代理都是开启aspectj动态代理创建的（即使没有接口也可以创建动态代理）
 *      对外暴露代理对象
 *  3）、本类调用对象
 *          OrderServiceImpl  o = (OrderServiceImpl) AopContext.currentProxy();
 *         o.b();
 *         o.c();
 *
 *   Seate控制分布式事务
 *   1）、每一个微服务先必须创建undo_log;
 *   2)、安装事务协调器 seate-server 从 https://github.com/seata/seata/releases,下载服务器软件包，将其解压缩。
 *   3)\整合
 *         1、导入依赖 spring-cloud-starter-alibaba-seata seata-all 0.7.1
 *         2\启动seata-server
 *          registry.conf:注册中心配置 修改registry type=nacos
 *          file.conf:
 *         3\所有想要用到分布式事务的微服务使用seata DataSourceProxy 代理自己的数据源
 *         4\每个微服务 都必须导入 file.conf registry.conf
 *           vgroup_mapping.{application.name}-fescar-service-group = "default"
 *         5\启动测试分布式方法
 *
 *         6 给分布式大事务的入口标注     @GlobalTransactional
 *         7 每一个远程的小事务用     @Transactional
 */
//@EnableAspectJAutoProxy(exposeProxy = true)
@EnableRabbit
@SpringBootApplication
@EnableDiscoveryClient
@EnableRedisHttpSession
@EnableFeignClients
public class GulimallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallOrderApplication.class, args);
    }

}
