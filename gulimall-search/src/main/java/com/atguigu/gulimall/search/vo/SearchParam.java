package com.atguigu.gulimall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * f封装页面所有可能传递过来的查询条件
 *
 * catalog3Id=225&keyword=小米&sort=saleCount_asc&hasStock=0/1&brandId=1&brandId=2
 */

@Data
public class SearchParam {

    private String keyword;//页面传递过来的全文匹配关键字
    private Long catalog3Id;//三级分类id

    /**
     * sort=saleCount_asc/desc
     * sort=skuPrice_asc/desc
     * sort=hotScore_asc/desc
     */
    private String sort;
    /**
     * 好多过滤条件
     * hasStock（是否有货）、skuPrice区间、brandId、cartalog3Id、attrs
     * hasStock=0、1
     * skuPrice=1_500 /_500/500_
     * brandId=1
     * attrs=2_5寸:6寸
     */
    private Integer hasStock;//是否只显示有货
    private String skuPrice;//价格区间查询
    private List<Long> brandId;//按照品牌进行查询 可以多选
    private List<String> attrs;//按照属性进行筛选
    private Integer pageNum=1;//页码

    private Integer testNum=0;


    private String _queryString;//原生的所有查询条件





}
