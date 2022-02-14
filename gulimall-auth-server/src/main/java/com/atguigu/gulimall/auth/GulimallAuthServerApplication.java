package com.atguigu.gulimall.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 核心原理
 * 1）@EnableRedisHttpSession导入RedisHttpSessionConfiguration配置
 *      1、给容器中添加了一个组件
 *          SessionRepository =》》》 【RedisOperationSessionRepository】--》redis操作session session的增删改查
 *      2、SessionRepositoryFilter--》Filter ：session 存储过滤器 每个请求过来都必须经过Filter
 *      3、以后获取session request.getSession();
 *      //sessionRepositoryWarpper
 *
 *      4、wrappedRequest.getSession();---> SessionRepository中获取到的
 *
 *   装饰者模式：
 *
 *   自动延期 ：redis中的数据也是有过期时间
 *
 *
 */

@EnableRedisHttpSession //整合redis作为session导入
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class GulimallAuthServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallAuthServerApplication.class, args);
    }

}
