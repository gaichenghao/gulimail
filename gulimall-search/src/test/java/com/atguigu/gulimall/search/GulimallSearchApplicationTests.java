package com.atguigu.gulimall.search;


import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;


@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallSearchApplicationTests {

    @Autowired
    private RestHighLevelClient client;


    @Test
    public  void contextLoads() {

        System.out.println(client);
    }

    /**
     * 测试存储数据到es
     */
    @Test
    public  void indexData() throws IOException {
        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1");//数据id
        //indexRequest.source("username","zhangsna","age","18","gender","男");
        User user=new User();
        String s = JSON.toJSONString(user);
        indexRequest.source(s, XContentType.JSON);//要保存的内容

        //执行操作
        IndexResponse index = client.index(indexRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
        //提取有用的响应数据
        System.out.println(index);
    }

    @Data
    class User{
        private String username;
        private String gender;
        private Integer age;
    }
}
