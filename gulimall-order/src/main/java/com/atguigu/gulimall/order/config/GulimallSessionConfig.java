package com.atguigu.gulimall.order.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class GulimallSessionConfig {
    @Bean
    public CookieSerializer cookieSerializer(){
        DefaultCookieSerializer defaultSerializer=new DefaultCookieSerializer();;

        defaultSerializer.setDomainName("gulimall.com");
        defaultSerializer.setCookieName("GULISESSION");
        return defaultSerializer;

    }

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer(){
        return  new GenericJackson2JsonRedisSerializer();
    }

}
