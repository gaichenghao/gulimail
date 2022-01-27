package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catalog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @GetMapping({"/","/index.html"})
    public String indexPage(Model model){
        
        //// TODO: 2022/1/25 查出所有的1级分类
        List<CategoryEntity> categories=categoryService.getLevel1Category();


        //试图解析器进行频串:
        //classpath:/templates/+返回值+：.html
        model.addAttribute("categorys",categories);
        return "index";
    }

    //index/catalog.json
    @ResponseBody
    @GetMapping("index/catalog.json")
    public Map<Long, List<Catalog2Vo>> getCatalogJson(){
        Map<Long, List<Catalog2Vo>> map=categoryService.getCatalogJson();
        return map;
    }

    @ResponseBody
    @GetMapping("/hello")
    private String hello(){
        return "hello";
    }



}
