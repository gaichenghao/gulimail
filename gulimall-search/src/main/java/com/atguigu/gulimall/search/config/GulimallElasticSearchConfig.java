package com.atguigu.gulimall.search.config;


import org.apache.http.HttpHost;
import org.elasticsearch.client.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 1\导入依赖
 * 2、编写配置,给容器中注入一个resthighlevelclient
 * 3、参照api
 */


@Configuration
public class GulimallElasticSearchConfig {

    public static final RequestOptions COMMON_OPTIONS;
    
    static {
        RequestOptions.Builder builder=RequestOptions.DEFAULT.toBuilder();
        //builder.addHeader("Authorization","Bearer "+TOKEN);
        //builder.setHttpAsyncResponseConsumerFactory(
        //        new HttpAsyncResponseConsumerFactory
        //                .HeapBufferedResponseConsumerFactory(30*1024*1024*1024);
                COMMON_OPTIONS=builder.build();

        
    }
    
    
    
    
    
    
    

    @Bean
    public RestHighLevelClient esRestClient(){

        RestClientBuilder builder=null;
        builder=RestClient.builder(new HttpHost("101.200.164.72",9200,"http"));
        RestHighLevelClient client=new RestHighLevelClient(builder);

        //RestHighLevelClient client=new RestHighLevelClient(
        //        RestClient.builder(
        //                new HttpHost("101.200.164.72",9200,"http")
        //));

        return client;

    }

}
