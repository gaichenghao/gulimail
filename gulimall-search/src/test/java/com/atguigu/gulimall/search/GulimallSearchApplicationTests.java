package com.atguigu.gulimall.search;


import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.search.bean.bank;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
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

    @Test
    public  void searchData() throws IOException {
        //1\创建检索请求
        SearchRequest searchRequest=new SearchRequest();
        //指定索引
        searchRequest.indices("bank");
        //指定dsl 检索条件
        //SearchSourceBuilder searchSourceBuilder 封装的条件
        SearchSourceBuilder searchSourceBuilder=new SearchSourceBuilder();
        //1.1 构造检索条件
        //searchSourceBuilder.query();
        //searchSourceBuilder.from();
        //searchSourceBuilder.size();
        //searchSourceBuilder.aggregation();
        searchSourceBuilder.query(QueryBuilders.matchQuery("address","mill"));


        //1.2)按照年龄的值分布进行聚合
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
        searchSourceBuilder.aggregation(ageAgg);
        //1.3 计算平均薪资
        AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
        searchSourceBuilder.aggregation(balanceAvg);

        System.out.println("检索条件"+searchSourceBuilder.toString());




        searchRequest.source(searchSourceBuilder);

        //2\执行检索：
        SearchResponse search = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
        //3分析结果
        System.out.println(search.toString());
        //Map map = JSON.parseObject(search.toString(), Map.class);
        //3.1) 获取所有查到的数据
        SearchHits hits = search.getHits();
        SearchHit[] searchHits = hits.getHits();

        for (SearchHit searchHit : searchHits) {
            //searchHit.getIndex();searchHit.getType();searchHit.getId();
            String sourceAsString = searchHit.getSourceAsString();
            bank bank = JSON.parseObject(sourceAsString, bank.class);
            System.out.println(bank.toString());
        }
        //3.2 获取这次检索到的分析数据
        Aggregations aggregations = search.getAggregations();
        //for (Aggregation aggregation : aggregations) {
        //
        //    System.out.println("当前聚合："+aggregation.getName());
        //
        //}
        Terms ageAgg1 = aggregations.get("ageAgg");
        for (Terms.Bucket bucket : ageAgg1.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println("年龄:"+keyAsString+"===>"+bucket.getDocCount());

        }
        Avg balanceAvg1 = aggregations.get("balanceAvg");
        System.out.println("平均薪资："+balanceAvg1.getValue());
    }


    /**
     * (1) 方便检索{
     *     skuId:1
     *     spuId:11
     *     skuTitle:华为xx
     *     price：998
     *     salecount：99
     *     attrs:[
     *      {尺寸：5寸}
     *      {cpu：高通945}
     *      {分辨率：全高清}
     *     ]
     * }
     * 冗余：
     * 100万*20=1000000*2kb=2g 20
     * （2）、
     * sku索引{
     *     skuId：1
     *     spuid：11
     *     xxxx
     * }
     * attr索引{
     *     spuId：11，
     *     attrs:{
     *         {尺寸：5寸}
     *         {cpu：高通545}
     *         {分辨率：全高清}
     *     }
     * }
     *
     *
     * 搜索小米：粮食 手机 电器
     *
     * 10000个 4000个spu
     * 分步。 4000个spu
     * esclient：spu：【4000个spuid】 4000*8=32000byte=32kb
     *
     * 32kb*10000=32000mb=32gb
     *
     *
     */







    @Data
    class User{
        private String username;
        private String gender;
        private Integer age;
    }
}
