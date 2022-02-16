package com.atguigu.gulimall.order.dao;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author gaich
 * @email gaichenghao@gmail.com
 * @date 2022-02-16 16:12:49
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
