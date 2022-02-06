package com.atguigu.gulimall.search.service;

import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;

public interface MallSearchService {

    /**
     *
     * @param param 检索的所有参数
     * @return 返回的检索结果，里面包含页面需要的所有信息
     */
    SearchResult search(SearchParam param);
}
