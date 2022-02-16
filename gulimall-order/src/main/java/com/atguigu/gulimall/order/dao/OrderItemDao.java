package com.atguigu.gulimall.order.dao;

import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单项信息
 * 
 * @author gaich
 * @email gaichenghao@gmail.com
 * @date 2022-02-16 16:12:49
 */
@Mapper
public interface OrderItemDao extends BaseMapper<OrderItemEntity> {
	
}
