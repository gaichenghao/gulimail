package com.atguigu.gulimall.search.vo;

import com.atguigu.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.List;
@Data
public class SearchResult {


    //查询到的商品信息
    private List<SkuEsModel> products;

    private Integer  pageNum;//当前页码
    private Long totel;//总数量
    private Integer pagePages;//总页码
    private List<Integer> pageNavs;

    private List<BrandVo> brands;//当前查询到的结果 所有涉及到的品牌
    private List<AttrVo> attrs;//当前查询到的结果 所有涉及到的所有属性

    private List<CatalogVo> catalogs;//当前查询到的结果 所有涉及到的所有分类

    //===================以上·是返回给页面的所有信息======================

    //面包屑导航数据
    private List<NavVo> navs;

    @Data
    public static class NavVo{
        private String navName;
        private String navValue;
        private String link;
    }

    @Data
    public static  class BrandVo{
        private Long brandId;
        private String brandName;
        private String brandImg;
    }

    @Data
    public static  class CatalogVo{
        private Long catalogId;
        private String catalogName;
    }

    @Data
    public static  class AttrVo{
        private Long attrId;
        private String attrName;
        private List<String> attrValue;
    }
}