package com.atguigu.gulimall.coupon.service;

import com.atguigu.common.to.SkuReductionTo;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.coupon.entity.SkuFullReductionEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * 商品满减信息
 *
 * @author gaich
 * @email gaichenghao@gmail.com
 * @date 2021-11-17 21:06:57
 */
public interface SkuFullReductionService extends IService<SkuFullReductionEntity> {


    PageUtils queryPage(Map<String, Object> params);


    void saveSkuReduction(SkuReductionTo skuReductionTo);

}

