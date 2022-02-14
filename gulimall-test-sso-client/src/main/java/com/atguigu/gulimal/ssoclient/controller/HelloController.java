package com.atguigu.gulimal.ssoclient.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Controller
public class HelloController {


    @Value("${sso.server.url}")
    String ssoServerUrl;

    /**
     * 无需登录就可访问
     * @return
     */
    @ResponseBody
    @GetMapping("/hello")
    public String hello(){
        return "hello";
    }

    /**
     * 感知登录成功返回的
     * @param model
     * @param session
     * @param token 只要登录成功了 调回来就会带上
     * @return
     */
    @GetMapping("/employees")
    public String employees(Model model, HttpSession session,
                            @RequestParam(value="token",required = false) String token){

        if(!StringUtils.isEmpty(token)){
            //去ssoserver 登录成功调回来就会带上
            // TODO: 2022/2/14 1\去ssoserver获取当前token真正对应的用户信息
            ClientHttpRequestFactory requestFactory;
            RestTemplate restTemplate=new RestTemplate();
            ResponseEntity<String> entity = restTemplate.getForEntity("http://sso.com:8188/userInfo?token=" + token, String.class);
            String body = entity.getBody();

            session.setAttribute("loginUser",body);
        }


        Object loginUser = session.getAttribute("loginUser");
        if(loginUser==null){
            //没登录 跳转到登录服务器进行登录

            //跳转过去以后 使用url上的查询参数辨识我们自己是哪个页面
            //redirect_url=http://client1.com:8080/employees
            return "redirect:"+ssoServerUrl+"?redirect_url=http://client1.com:8181/employees";
        }else {

            List<String> emps=new ArrayList<>();
            emps.add("张三");
            emps.add("李四");

            model.addAttribute("emps",emps);
            return "list";

        }




    }


}
