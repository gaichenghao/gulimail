package com.atguigu.gulimall.member.dao;

import com.atguigu.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author gaich
 * @email gaichenghao@gmail.com
 * @date 2021-11-17 21:21:16
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
