package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.SkuInfoDao;
import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import com.atguigu.gulimall.product.service.*;
import com.atguigu.gulimall.product.vo.SkuItemSaleAttrVo;
import com.atguigu.gulimall.product.vo.SkuItemVo;
import com.atguigu.gulimall.product.vo.SpuItemAttrGroupVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Autowired
    SkuImagesService imagesService;

    @Autowired
    SpuInfoDescService spuInfoDescService;

    @Autowired
    AttrGroupService attrGroupService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuInfo(SkuInfoEntity skuInfoEntity) {
        this.baseMapper.insert(skuInfoEntity);

    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {

        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();
        String key= (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.and(w->{
                w.eq("sku_id",key).or().like("sku_name",key);
            });
        }

        String catelogId= (String) params.get("catelogId");
        if(!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)){



            wrapper.eq("catalog_id",catelogId);

        }
        String brandId= (String) params.get("brandId");
        if(!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)){
            wrapper.eq("brand_id",brandId);
        }
        String min= (String) params.get("min");
        BigDecimal decimal = new BigDecimal(min);
        if(!StringUtils.isEmpty(min)){

            try {
                BigDecimal decimal1 = new BigDecimal(min);
                if(decimal.compareTo(new BigDecimal("0"))==1){
                    wrapper.ge("price",min);
                }
            }catch (Exception ex){

            }

        }
        String max= (String) params.get("max");
        if(!StringUtils.isEmpty(max) && !"0".equalsIgnoreCase(max)){
            wrapper.le("price",max);
        }



        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                wrapper
        );
        /**
         * key:
         * catelogId: 0
         * brandId: 0
         * min: 0
         * max: 0
         */

        return new PageUtils(page);
    }

    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {
        List<SkuInfoEntity> list = this.list(
                new QueryWrapper<SkuInfoEntity>().eq("spu_id",spuId));


        return list;
    }

    @Override
    public SkuItemVo item(Long skuId) {
        SkuItemVo skuItemVo=new SkuItemVo();
        //1 sku基本信息查询  pms_info_info
        SkuInfoEntity info = getById(skuId);
        skuItemVo.setInfo(info);
        Long catalogId = info.getCatalogId();
        Long spuId = info.getSpuId();


        //2 sku的图片信息 pms_sku_images
        List<SkuImagesEntity> images=imagesService.getImagesBySkuId(skuId);

        skuItemVo.setImages(images);
        //3 获取spu的销售属性组合。
        List<SkuItemSaleAttrVo>  saleAttrVos=skuSaleAttrValueService.getSaleAttrsBySpuId(spuId);
        skuItemVo.setSaleAttr(saleAttrVos);


        // 4\获取spu的介绍
        SpuInfoDescEntity spuInfoDescEntity = spuInfoDescService.getById(skuId);
        skuItemVo.setDesp(spuInfoDescEntity);

        // 5 获取spu的规格参数信息
        List<SpuItemAttrGroupVo> attrGroupVos=attrGroupService.getAttrGroupWithAttrBySpuId(spuId,catalogId);
        skuItemVo.setGroupAttrs(attrGroupVos);

        return skuItemVo;
    }

}