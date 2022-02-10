package com.atguigu.gulimall.member;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
public  class GulimallMemberApplicationTests {

    @Test
    public  void contextLoads() {

        //抗修改性 彩虹表 123456->xxxxx 234567->dddddd
        String s = DigestUtils.md5Hex("123456");

        //md5 不能直接进行密码的加密存储

        //盐值加密：随机值 加盐 $1$+8位字符
        //$1$w7wSTvxz$Wcq/sNLKihe91iyjJBwF9.
        //$1$qqqqqqqq$AZofg3QwurbxV3KEOzwuI1
        //验证：123456进行盐值（去数据库查）加密
        //String s1 = Md5Crypt.md5Crypt("123456".getBytes(),"$1$qqqqqqqq");
        //
        //System.out.println(s1);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        //$2a$10$wFS8gF4emr2tbdXYC2q1uuxZv4TiX9JKInSJs/.qlFbdRzF3wYEfK
        String encode = passwordEncoder.encode("123456");
        System.out.println(encode);
        boolean matches = passwordEncoder.matches("123456", "$2a$10$wFS8gF4emr2tbdXYC2q1uuxZv4TiX9JKInSJs/.qlFbdRzF3wYEfK");
        System.out.println(matches);


    }

}
