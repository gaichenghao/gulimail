package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.gulimall.auth.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

/**
 * 处理社交登录请求
 */
@Controller
public class OAuth2Controller {


    @GetMapping("/oauth2.0/weibo/success")
    public String weibo(@RequestParam("code") String code) throws Exception {

        Map<String, String> map=new HashMap<>();
        map.put("client","");
        map.put("client_secret","");
        map.put("grant_type","");
        map.put("redirect_uri","");
        map.put("code",code);

        //1、根据code换取accessToken；
        HttpResponse response = HttpUtils.doPost("api.weibo.com", "", "post", null, null, map);
        //2、 处理
        if(response.getStatusLine().getStatusCode()==200){
            //获取到了accessToken
            String json = EntityUtils.toString(response.getEntity());
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);

            //知道当前是那个社交用户
            //1）、当前用户如果是第一次进网站 自动注册寄来（为当前社交用户生成一个会员信息账号 以后这个社交正好就对应指定的会员）
            //登陆或者注册
        }else {
            return "redirect:http://auth.gulimall.com/login.html";
        }

        //2、登陆成功就跳回首页
        return "redirect:http://gulimall.com";

    }

    @GetMapping("/oauth2.0/gitee/success")
    public String gitee(){

        return "";

    }
}
