package com.atguigu.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.feign.ProductFeignService;
import com.atguigu.gulimall.search.service.MallSearchService;
import com.atguigu.gulimall.search.vo.AttrResponseVo;
import com.atguigu.gulimall.search.vo.BrandVo;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class MallSearchServiceImpl implements MallSearchService {


    @Autowired
    private RestHighLevelClient client;


    @Autowired
    private ProductFeignService productFeignService;


    //1???es?????????
    @Override
    public SearchResult search(SearchParam param) {
        //1?????????????????????????????????dsl??????
        SearchResult result=null;

        //1\??????????????????
        SearchRequest searchRequest=buildSearchRequest(param);

        try {
            //2\??????????????????
            SearchResponse response = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
            //3\????????????????????????????????????????????????
            result=bulidSearchResult(response,param);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * ??????????????????
     * #???????????? ????????????????????? ?????? ?????? ???????????? ????????? ?????? ?????? ?????? ????????????
     * @return
     * @param param
     */
    private SearchRequest buildSearchRequest(SearchParam param) {

        SearchSourceBuilder sourceBuilder=new SearchSourceBuilder();//??????dsl?????????

        /**
         * ????????????????????? ????????????????????? ?????? ?????? ???????????? ?????????
         */
        //1\??????bool -query
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //1.1 must-????????????
        if(!StringUtils.isEmpty(param.getKeyword())){
            boolQuery.must(QueryBuilders.matchQuery("skuTitle",param.getKeyword()));
        }
        //1.2 bool -filter -??????????????????id??????
        if(param.getCatalog3Id()!=null){
            boolQuery.filter(QueryBuilders.termQuery("catalogId",param.getCatalog3Id()));
        }
        //1.2 bool-filter -????????????id??????
        if(param.getBrandId()!=null && param.getBrandId().size()>0){
            boolQuery.filter(QueryBuilders.termsQuery("brandId",param.getBrandId()));
        }
        // 1.2 bool-filter -??????????????????????????????
        if(param.getAttrs()!=null && param.getAttrs().size()>0){
            for (String attrStr : param.getAttrs()){

                //attrs=1_5??????8??? & attrs=2_6G:8g
                BoolQueryBuilder nestesBoolQuery = QueryBuilders.boolQuery();
                //attr-1_5??????8???
                String[] s = attrStr.split("_");
                String attrId=s[0];
                String[] attrValues=s[1].split(":");
                nestesBoolQuery.must(QueryBuilders.termQuery("attrs.attrId",attrId));
                nestesBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue",attrValues));
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestesBoolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }
        // 1.2 bool-filter -?????????????????????????????????
        if(param.getHasStock()!=null){
            boolQuery.filter(QueryBuilders.termQuery("hasStock",param.getHasStock()==1));
        }

        // 1.2 bool-filter -??????????????????????????????
        if(!StringUtils.isEmpty(param.getSkuPrice())){
            //1_500 _500 500_
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");

            String[] s = param.getSkuPrice().split("_");
            if(s.length==2){
                //??????
                rangeQuery.gte(s[0]).lte(s[0]);
            }else if(s.length==1){
                if(param.getSkuPrice().startsWith("_")){
                    rangeQuery.lte(s[0]);
                }
                if(param.getSkuPrice().endsWith("_")){
                    rangeQuery.gte(s[0]);
                }
            }


        }
        //??????????????????????????????????????????
        sourceBuilder.query(boolQuery);
        /**
         * ?????? ?????? ??????
         */
        //2.1 ??????
        if(!StringUtils.isEmpty(param.getSort())){
            String sort = param.getSort();
            //sort=hotScore_asc/desc
            String[] s = sort.split("_");
            SortOrder order = s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            sourceBuilder.sort(s[0],order);
        }
        //2.2 ??????
        sourceBuilder.from((param.getPageNum()-1)*EsConstant.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);
        //2.3 ??????
        if(!StringUtils.isEmpty(param.getKeyword())){
            HighlightBuilder builder = new HighlightBuilder();
            builder.field("skuTitle");
            builder.preTags("<b style='color:red'>");
            builder.postTags("</b>");
            sourceBuilder.highlighter(builder);
        }
        /**
         * ????????????
         */
        //1???????????????
        if(param.getTestNum()!=-1){
            if(param.getTestNum()==1 || param.getTestNum()==0){
                TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg").field("brandId").size(50);
//                brand_agg.field("brandId").size(50);
                //????????????????????????
                brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
                brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
                //TODO 1\??????brand
                sourceBuilder.aggregation(brand_agg);
            }
            //2\???????????? catalog_agg
            if(param.getTestNum()==2 || param.getTestNum()==0){
                TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
                catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
                //TODO 2\??????catalog
                sourceBuilder.aggregation(catalog_agg);
            }
            if(param.getTestNum()==3 || param.getTestNum()==0){
                //3\???????????? attr_agg
                NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");

                //????????????????????????attrId
                TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
                //?????????????????????attr_id???????????????
                attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
                //?????????????????????attr_id?????????????????????????????????attrvalue
                attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
                attr_agg.subAggregation(attr_id_agg);
                //TODO 2\??????attr
                sourceBuilder.aggregation(attr_agg);
            }
        }
        String string = sourceBuilder.toString();
        System.out.println("?????????dsl???======="+string);

        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder);
        return searchRequest;


    }

    /**
     * ??????????????????
     * @param response
     * @param param
     * @return
     */
    private SearchResult bulidSearchResult(SearchResponse response, SearchParam param) {

        SearchResult result=new SearchResult();
        //1\?????????????????????????????????
        List<SkuEsModel> esModels=new ArrayList<>();
        SearchHits hits = response.getHits();
        if(hits.getHits()!=null && hits.getHits().length>0){
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel esModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                if(!StringUtils.isEmpty(param.getKeyword())){
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String string = skuTitle.getFragments()[0].string();
                    esModel.setSkuTitle(string);
                }
                esModels.add(esModel);
            }
        }
        result.setProducts(esModels);
        //2\???????????????????????????????????????????????????
        List<SearchResult.AttrVo>attrVos=new ArrayList<>();
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo=new SearchResult.AttrVo();

            //???????????????id
            long attrId = bucket.getKeyAsNumber().longValue();
            //?????????????????????
            String attrName = ((ParsedStringTerms) bucket.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();
            //????????????????????????
            List<String> attrValues = ((ParsedStringTerms) bucket.getAggregations().get("attr_value_agg")).getBuckets().stream().map(item -> {
                String keyAsString = ((Terms.Bucket)item).getKeyAsString();
                return  keyAsString;
            }).collect(Collectors.toList());
            attrVo.setAttrId(attrId);
            attrVo.setAttrName(attrName);
            attrVo.setAttrValue(attrValues);


            attrVos.add(attrVo);
        }

        result.setAttrs(attrVos);
        //3\???????????????????????????????????????????????????
        List<SearchResult.BrandVo> brandVos=new ArrayList<>();
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        for (Terms.Bucket bucket : brand_agg.getBuckets()) {
            SearchResult.BrandVo brandVo=new SearchResult.BrandVo();
            //1\???????????????id
            long brandId = bucket.getKeyAsNumber().longValue();
            //2\?????????????????????
            String brandname = ((ParsedStringTerms) bucket.getAggregations().get("brand_name_agg")).getBuckets().get(0).getKeyAsString();
            //3\?????????????????????
            String brandImg = ((ParsedStringTerms) bucket.getAggregations().get("brand_img_agg")).getBuckets().get(0).getKeyAsString();
            brandVo.setBrandId(brandId);
            brandVo.setBrandName(brandname);
            brandVo.setBrandImg(brandImg);
            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);
        //4\???????????????????????????????????????????????????
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");
        List<SearchResult.CatalogVo> catalogVos=new ArrayList<>();
        List<? extends Terms.Bucket> buckets = catalog_agg.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            //????????????id
            String keyAsString = bucket.getKeyAsString();
            catalogVo.setCatalogId(Long.parseLong(keyAsString));
            //???????????????
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalog_name = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalog_name);
            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);
        //5\????????????-??????
        result.setPageNum(param.getPageNum());
        //5\????????????-????????????
        long total = hits.getTotalHits().value;
        result.setTotel(total);
        //5\????????????-????????? ?????? 11/2=5.??????1
        int totalPages=(int)total%EsConstant.PRODUCT_PAGESIZE>0?(int)total/EsConstant.PRODUCT_PAGESIZE+1:(int)total/EsConstant.PRODUCT_PAGESIZE;
        result.setPagePages(totalPages);

        List<Integer> pageNavs=new ArrayList<>();
        for (int i=1;i<=totalPages ;i++){
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);

        //6???????????????????????????
        if(param.getAttrs()!=null && param.getAttrs().size()>0){
            List<SearchResult.NavVo> collect = param.getAttrs().stream().map(attr -> {
            //1/????????????attrs?????????????????????
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            //attes=2_5??????6???
            String[] s = attr.split("_");
            navVo.setNavValue(s[1]);
            R r = productFeignService.attrInfo(Long.parseLong(s[0]));
                result.getAttrIds().add(Long.parseLong(s[0]));
            if(r.getCode()==0){
                AttrResponseVo data = r.getData("attr", new TypeReference<AttrResponseVo>() {
                });
                navVo.setNavName(data.getAttrName());
            }else{
                navVo.setNavName((s[0]));
            }


            //2????????????????????????????????? ??????????????????????????????,??????????????????url?????????????????????
            //??????????????????????????? ???????????????
            //attrs=15_??????
            String replace = replaceQueryString(param, attr,"attrs");
            navVo.setLink("http://search.gulimall.com/list.html?"+replace);
            //navVo.setNavName();
            return navVo;
        }).collect(Collectors.toList());
            result.setNavs(collect);
        }

        //?????? ??????
        if(param.getBrandId()!=null && param.getBrandId().size()>0){
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo navVo=new SearchResult.NavVo();

            navVo.setNavName("??????");
            //// TODO: 2022/2/7 ????????????????????????
            R r = productFeignService.brandInfo(param.getBrandId());
            if(r.getCode()==0){
                List<BrandVo> brandVoList = r.getData("brand", new TypeReference<List<BrandVo>>() {
                });
                StringBuffer stringBuffer=new StringBuffer();
                String replace="";
                for (BrandVo brandVo : brandVoList) {
                    stringBuffer.append(brandVo.getName()+";");
                    replace=replaceQueryString(param,brandVo.getBrandId()+"","brandId");
                }
                navVo.setNavValue(stringBuffer.toString());
                navVo.setLink("http://search.gulimall.com/list.html?"+replace);
            }
            navs.add(navVo);
        }

        //TODO ??????????????????????????????

        return result;
    }

    private String replaceQueryString(SearchParam param, String value,String key) {
        String encode=null;
        try {
            encode = URLEncoder.encode(value, "UTF-8");
            encode=encode.replace("+","%20");//??????????????????????????????java?????????
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String replace = param.get_queryString().replace("&"+key+"=" + encode, "");
        return replace;
    }


}
