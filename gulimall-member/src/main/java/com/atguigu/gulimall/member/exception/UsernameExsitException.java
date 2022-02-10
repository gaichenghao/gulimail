package com.atguigu.gulimall.member.exception;

public class UsernameExsitException extends RuntimeException{


    public UsernameExsitException() {
        super("用户名存在");
    }
}
