package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.AttrGroupEntity;
import com.atguigu.gulimall.product.vo.SpuItemAttrGroupVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 属性分组
 * 
 * @author gaich
 * @email gaichenghao@gmail.com
 * @date 2021-11-15 22:04:24
 */
@Mapper
public interface AttrGroupDao extends BaseMapper<AttrGroupEntity> {

    List<SpuItemAttrGroupVo>
    getAttrGroupWithAttrBySpuId(@Param("spuId") Long spuId,@Param("catalogId") Long catalogId);
}
