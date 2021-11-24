package com.atguigu.gulimall.coupon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 1\如何使用nacos作为配置中心统一管理配置
 *
 * 1）、引入依赖        <dependency>
 *             <groupId>com.alibaba.cloud</groupId>
 *             <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
 *         </dependency>
 * 2）、创建一个bootstrap.properties 固定名
 * spring.application.name=gulimall-coupon 服务器名字
 *
 * spring.cloud.nacos.config.server-addr=127.0.0.1:8848 nacos地址
 *
 * 3）、需要给配置中心默认添加一个叫数据集（Data Id） gulimall-coupon。properties 默认规则：应用名.properties
 * 4)给应用名.properties添加任何配置
 * 5）、动态获取配置
 * @RefreshScope //自动刷新nacos配置文件
 * @Value("${配置项的名}")
 * 如果配置中心与项目配置文件都配置了相同的项，优选使用配置中心的配置
 *
 * 2、细节
 *  1）、命名空间
 *      默认：publiv（保留空间）：默认新增的所有配置都在public空间
 *      1、开发，测试，生成 利用命名空间做环境变量
 *          注意：在bootstrap.properties;配置上，需要使用那个命名空间的配置
 *          spring.cloud.nacos.config.namespace=581dd9d5-247d-4105-914a-64ab9ed12740
 *      2、每一个微服务之间相互隔离配置，每一个微服务都创建自己的命名空降，值加载自己命名空间下的所有配置
 *  2）、配置集：所有配置集合
 *  3）、配置集id：类似配置文件名
 *  4）、配置分组：
 *  默认所有的·配置集都属于：default_group
 *      1111,0618
 *
 *    每个微服务创建自己的命名空间 使用配置分组区分环境，dev，test
 *  3、同时加载多个配置集
 *  1)、微服务任何配置信息，任何配置文件都可以放在配置中心中
 *  2）、只需要bootstrap.properties说明加载配置中心中那些配置文件即可
 * 3）、@Value，@ConfiguationProperties。。。
 * 以前springboot任何方式从配置文件中获取值 都可以使用
 *
 */

@SpringBootApplication
@EnableDiscoveryClient
public class GulimallCouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallCouponApplication.class, args);
    }

}
