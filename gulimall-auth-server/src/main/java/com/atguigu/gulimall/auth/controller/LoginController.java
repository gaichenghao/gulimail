package com.atguigu.gulimall.auth.controller;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.feign.ThirdPartFeignService;
import com.atguigu.gulimall.auth.vo.UserRegistVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController {


    @Autowired
    ThirdPartFeignService thirdPartFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;




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
        //3 redis 缓存验证码 防止同一个phone在60秒内再次发送验证码
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX+phone,code,10, TimeUnit.MINUTES);

        thirdPartFeignService.SendCode(phone,code);
        return R.ok();
    }


    /**
     * @Valid 开启校验
     *
     * @param vo
     * @param result
     * @return
     */

    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result, Model model){
        if(result.hasErrors()){
            //校验出错 转发到注册页
            return "forward:/reg.html";
        }
        //注册成功到首页 回到登录页
        return "redirect:/login.html";

    }
}
