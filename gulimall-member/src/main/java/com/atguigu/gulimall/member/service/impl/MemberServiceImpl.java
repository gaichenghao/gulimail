package com.atguigu.gulimall.member.service.impl;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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
    public MemberEntity login(SocialUser socialUser) {
        //登陆和注册合并逻辑
        String uuid = socialUser.getUuid();
        //1\判断当前社交用户是否 已经登录过系统
        MemberDao member=this.baseMapper;
        MemberEntity entity = member.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uuid));


        return null;
    }


}