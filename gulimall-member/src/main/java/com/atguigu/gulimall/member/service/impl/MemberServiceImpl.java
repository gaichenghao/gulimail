package com.atguigu.gulimall.member.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.member.dao.MemberDao;
import com.atguigu.gulimall.member.dao.MemberLevelDao;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.atguigu.gulimall.member.exception.PhoneExsitException;
import com.atguigu.gulimall.member.exception.UsernameExsitException;
import com.atguigu.gulimall.member.service.MemberService;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberRegistVo;
import com.atguigu.gulimall.member.vo.SocialUser;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Map;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegistVo vo) {
        MemberDao baseMapper = this.baseMapper;
        MemberEntity entity=new MemberEntity();
        //设置默认等级
        MemberLevelEntity levelEntity =memberLevelDao.getDefaultLevel();
        if(levelEntity!=null){
            entity.setLevelId(levelEntity.getId());
        }else {
            entity.setLevelId(-1L);
        }


        //检查用户名和手机是否唯一.为了让controller能感知异常 异常机制
        checkPhoneUnique(vo.getPhone());
        checkUserNameUnique(vo.getUserName());

        entity.setMobile(vo.getPhone());
        entity.setUsername(vo.getUserName());

        //密码要进行加密储存
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassword());
        entity.setPassword(encode);

        //其他的默认信息

        //保存
        baseMapper.insert(entity);

    }

    @Override
    public void checkPhoneUnique(String phone) throws  PhoneExsitException{

        MemberDao baseMapper = this.baseMapper;
        Integer count = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if(count>0){
            throw new  PhoneExsitException();
        }
    }

    @Override
    public void checkUserNameUnique(String username) throws  UsernameExsitException{
        MemberDao baseMapper = this.baseMapper;
        Integer count = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
        if(count>0){
            throw new UsernameExsitException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginacct = vo.getLoginacct();

        String password = vo.getPassword();

        MemberDao baseMapper = this.baseMapper;
        MemberEntity entity = baseMapper
                .selectOne(new QueryWrapper<MemberEntity>()
                        .eq("username", loginacct).or().eq("mobile", loginacct));
        if(entity==null){
            //登录失败
            return null;
        }else{
            //1\获取到数据库的password
            String passwordDb=entity.getPassword();
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            //2\密码匹配
            boolean matches = encoder.matches(vo.getPassword(),passwordDb);
            if(matches){
                return entity;
            }else {
                return null;
            }

        }
    }

    @Override
    public MemberEntity login(SocialUser socialUser) throws Exception {
        //gitee获取uid
        //根据accessToken 获取uid
        Map<String, String> userMap=new HashMap<>();
        userMap.put("access_token",socialUser.getAccess_token());
        HttpResponse userinfoResponse = HttpUtils.doGet("https://gitee.com", "/api/v5/user", "get",  new HashMap<>(), userMap);

        JSONObject jsonObject=new JSONObject();
        if(userinfoResponse.getStatusLine().getStatusCode()==200){
            String userInfo = EntityUtils.toString(userinfoResponse.getEntity());
            jsonObject = JSONObject.parseObject(userInfo);
            String id = jsonObject.getString("id");
            socialUser.setUuid(id);
        }else {
            throw new  LoginException();
        }

        //登陆和注册合并逻辑
        String uuid = socialUser.getUuid();
        //1\判断当前社交用户是否 已经登录过系统
        MemberDao member=this.baseMapper;
        MemberEntity memberEntity = member.selectOne(new QueryWrapper<MemberEntity>().eq("aocial_uid", uuid));

        if(memberEntity!=null){
            //这个用户已经注册
            MemberEntity update = new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(socialUser.getAccess_token());
            update.setExpiresIn(socialUser.getExpires_in());
            member.updateById(update);

            memberEntity.setAccessToken(socialUser.getAccess_token());
            memberEntity.setExpiresIn(socialUser.getExpires_in());
            return memberEntity;
        }else {
            //2\没有查到当前社交用户对应的记录我们就需要注册一个
            MemberEntity regist=new MemberEntity();
            try {
                Map<String, String> query=new HashMap<>();
                query.put("access_token",socialUser.getAccess_token());
                //query.put("uid",socialUser.getUuid());
                //HttpResponse response = HttpUtils.doGet("https://api.weibo.com", "/2/users/show.json", "get", new HashMap<>(), query);
                if(userinfoResponse.getStatusLine().getStatusCode()==200){
                    //昵称
                    String name = jsonObject.getString("name");
                    //String gender = jsonObject.getString("gender");
                    //.........
                    regist.setNickname(name);
                    //regist.setGender("m".equals(gender)?1:0);
                    //........
                }

            }catch (Exception ex){
                log.error(ex.getMessage());
            }
            regist.setAocialUid(socialUser.getUuid());
            regist.setAccessToken(socialUser.getAccess_token());
            regist.setExpiresIn(socialUser.getExpires_in());
            member.insert(regist);
            return regist;
        }


    }


}