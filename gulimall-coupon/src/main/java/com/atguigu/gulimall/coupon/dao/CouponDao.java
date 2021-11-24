package com.atguigu.gulimall.coupon.dao;

import com.atguigu.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author gaich
 * @email gaichenghao@gmail.com
 * @date 2021-11-17 21:06:57
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
