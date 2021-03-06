package com.atguigu.gulimall.product.vo;


import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {

    //1 sku基本信息查询  pms_info_info
    SkuInfoEntity info;

    private boolean hasStock = true;

    //2 sku的图片信息 pms_sku_images
    List<SkuImagesEntity> images;
    //3 获取spu的销售属性组合。
    List<SkuItemSaleAttrVo> saleAttr;
    // 4\获取spu的介绍
    SpuInfoDescEntity desc;
    // 5 获取spu的规格参数信息
    List<SpuItemAttrGroupVo> groupAttrs;




    //@Data
    //public static class SkuItemSaleAttrVo{
    //    private Long attrId;
    //    private String attrName;
    //    private List<String> attrValues;
    //}
    //@ToString
    //@Data
    //public static class SpuItemAttrGroupVo{
    //    private String groupName;
    //    private List<SpuBaseAttrVo> attrs;
    //
    //}
    //@ToString
    //@Data
    //public static class SpuBaseAttrVo{
    //    private String attrName;
    //    private String attrValue;
    //}

    //6、秒杀商品的优惠信息
    SeckillSkuVo seckillSkuVo;





}
