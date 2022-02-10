package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.feign.ThirdPartFeignService;
import com.atguigu.gulimall.auth.vo.UserLoginVo;
import com.atguigu.gulimall.auth.vo.UserRegistVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {


    @Autowired
    ThirdPartFeignService thirdPartFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;


    @Autowired
    MemberFeignService memberFeignService;



    //@GetMapping("/login.html")
    //public String loginPage(){
    //    return "login";
    //}



    /**
     * 发送一个请求 直接跳转到一个页面中
     * SpringMvc viewcontroller 将请求和页面映射过来
     *
     *
     */
    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone){

        // TODO 1接口防刷
        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);

        if(!StringUtils.isEmpty(redisCode)){
            long l = Long.parseLong(redisCode.split("_")[1]);
            if(System.currentTimeMillis()-l<60000){
                //60秒内不能再发
                return R.error(BizCodeEnume.SMS_EXCEPTION.getCode(), BizCodeEnume.SMS_EXCEPTION.getMsg());
            }
        }
        //2 验证码的再次校验 redis 存key-phone value-code sms-code sms-code:13167590000->45678
        String code = UUID.randomUUID().toString().substring(0, 5);
        String subString=code+"_"+System.currentTimeMillis();
        //3 redis 缓存验证码 防止同一个phone在60秒内再次发送验证码
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX+phone,subString,10, TimeUnit.MINUTES);

        thirdPartFeignService.SendCode(phone,code);
        return R.ok();
    }


    /**
     * @Valid 开启校验
     * 
     * // TODO: 2022/2/10  重定向携带数据 利用session原理 将数据放在session中。 
     *  只要跳到下一个页面取出这个数据以后 session里面的数据就会删掉
     *  
     *  // TODO: 2022/2/10  分布式下的session模式
     *
     * RedirectAttributes redirectAttributes 模拟重定向携带数据
     *
     * @param vo
     * @param result
     * @return
     */

    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result,
                         RedirectAttributes redirectAttributes,
                         HttpSession session){
        if(result.hasErrors()){
            /**
             * .map(fieldError -> {
             *                 String field=fieldError.getField();
             *                 String description=fieldError.getDefaultMessage();
             *                 errors.put(field,description);
             *                 result
             *             })
             */
            //Map<String, String> errors = result.getFieldErrors().stream().collect(
            //        Collectors.toMap(fieldError -> {
            //                    return fieldError.getField();
            //                },
            //                fieldError -> {
            //                    return fieldError.getDefaultMessage();
            //                }));

            Map<String, String> errors = result.getFieldErrors().stream().collect(
                    Collectors.toMap(FieldError::getField,FieldError::getDefaultMessage));
            //model.addAttribute("errors",errors);
            redirectAttributes.addFlashAttribute("errors",errors);
            //request method post not supported
            //用户注册->/regist[post] ---->转发/reg_copy.html (路径映射默认都是get方式访问的)
            //return "forward:/reg_copy.html";
            //session.set
            //校验出错 转发到注册页
            return "redirect:http://auth.gulimall.com/reg.html";
        }

        //// TODO: 2022/2/10 测试数据 需要删除
        //vo.setUserName("gaich");
        //vo.setPassword("123456");
        //vo.setPhone("13167590654");
        //vo.setCode("4cdcb");

        //1.校验验证码
        String code = vo.getCode();
        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if(!StringUtils.isEmpty(s)){
            if(code.equals(s.split("_")[0])){
                //删除验证码
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
                //验证码通过         //真正注册 调用远程服务进行注册
                R r = memberFeignService.regist(vo);
                if(r.getCode()==0){
                    //成功
                    return   "redirect:/login.html";
                }else{
                    Map<String, String> errors=new HashMap<>();
                    errors.put("msg",r.getData("msg",new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("errors",errors);
                    return "redirect:http://auth.gulimall.com/reg.html";
                }


            }else{
                Map<String, String> errors=new HashMap<>();
                errors.put("code","验证码错误");
                redirectAttributes.addFlashAttribute("errors",errors);
                //校验出错，转发到注册页
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        }else {
            Map<String, String> errors=new HashMap<>();
            errors.put("code","验证码错误");
            redirectAttributes.addFlashAttribute("errors",errors);
            //校验出错，转发到注册页
            return "redirect:http://auth.gulimall.com/reg.html";
        }


    }



    @PostMapping("/login")
    public String login(UserLoginVo vo,RedirectAttributes redirectAttributes){
        //远程登录
        R r = memberFeignService.login(vo);
        if(r.getCode()==0){

            //成功
            return  "redirect:http://gulimall.com";
        }else{

            Map<String, String> errors=new HashMap<>();
            errors.put("msg",r.getData("msg",new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:http://auth.gulimall.com/login.html";

        }

    }



}
