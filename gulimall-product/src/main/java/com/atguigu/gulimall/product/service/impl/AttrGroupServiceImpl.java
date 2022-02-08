package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.entity.AttrEntity;
import com.atguigu.gulimall.product.entity.AttrGroupEntity;
import com.atguigu.gulimall.product.service.AttrGroupService;
import com.atguigu.gulimall.product.service.AttrService;
import com.atguigu.gulimall.product.vo.AttrGroupWithAttrVo;
import com.atguigu.gulimall.product.vo.SpuItemAttrGroupVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        String key = (String) params.get("key");
        //select * from pms_attr_group where catelog_id=? and (attr_group_id=key or attr_group_name like %key%)
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();
        if(!StringUtils.isEmpty(key)){

            wrapper.and((obj)->{
                obj.eq("attr_group_id",key).or().like("attr_group_name",key);
            });
        }
        if (catelogId==0){
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params),
                    wrapper);
            return  new PageUtils(page);
        }else {
            wrapper.eq("catelog_id",catelogId);
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params),
                    wrapper);
            return  new PageUtils(page);
        }
    }


    /**
     * 根据分类id查出所有的分组以及这些组里面的属性
     * @param catelogId
     * @return
     */
    @Override
    public List<AttrGroupWithAttrVo> getAttrGroupWithAttrByCatelogId(Long catelogId) {
        //1\查询分组信息
        List<AttrGroupEntity> catelog_id = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        List<AttrGroupWithAttrVo> collect = catelog_id.stream().map(item -> {

            AttrGroupWithAttrVo attrVo=new AttrGroupWithAttrVo();
            BeanUtils.copyProperties(item,attrVo);
            List<AttrEntity> attr = attrService.getRelationAttr(attrVo.getAttrGroupId());
            attrVo.setAttrs(attr);

            return attrVo;
        }).collect(Collectors.toList());

        return collect;
    }



    @Override
    public List<SpuItemAttrGroupVo> getAttrGroupWithAttrBySpuId(Long spuId, Long catalogId) {
        //1、 查出当前spu对应的所有属性的分组信息以及 当前分组的所有属性对应的字
        //1)、
        AttrGroupDao baseMapper = this.getBaseMapper();
        List<SpuItemAttrGroupVo> vos=baseMapper.getAttrGroupWithAttrBySpuId(spuId,catalogId);

        return vos;
    }

}