package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.constant.ProductConstant;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.atguigu.gulimall.product.dao.AttrDao;
import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gulimall.product.entity.AttrEntity;
import com.atguigu.gulimall.product.entity.AttrGroupEntity;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.AttrService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.AttrGroupRelationVo;
import com.atguigu.gulimall.product.vo.AttrRespVo;
import com.atguigu.gulimall.product.vo.AttrVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    CategoryDao categoryDao;

    @Autowired
    CategoryService categoryService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );
        PageUtils pageUtils = new PageUtils(page);
        List<AttrEntity> records = page.getRecords();
        List<AttrRespVo> respVoList = records.stream().map((attrEntity) -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);
            //1设置分类和分组的名字
            AttrAttrgroupRelationEntity attr_id = attrAttrgroupRelationDao.selectOne(
                    new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));
            if (attr_id != null) {
                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attr_id.getAttrGroupId());
                attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
            }

            CategoryEntity category = categoryDao.selectById(attrEntity.getCatelogId());
            if (category != null) {
                attrRespVo.setCatelogName(category.getName());
            }


            return attrRespVo;
        }).collect(Collectors.toList());

        pageUtils.setList(respVoList);
        return pageUtils;
    }

    @Transactional
    @Override
    public void saveAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        //attrEntity.setAttrName(attr.getAttrName());
        BeanUtils.copyProperties(attr,attrEntity);
        //1保存基本数据
        this.save(attrEntity);
        //2保存关联关系
        if(attr.getAttrType()== ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() && attr.getAttrGroupId()!=null){
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            relationEntity.setAttrId(attrEntity.getAttrId());
            attrAttrgroupRelationDao.insert(relationEntity);
        }

    }

    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId,String type) {
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>()
                .eq("attr_type","base".equalsIgnoreCase(type)?
                        ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode():ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());

        if(catelogId!=0){
            wrapper.eq("catelog_Id",catelogId)                   ;
        }

        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.and((wr)->{
                wr.eq("attr_id",key).or().like("attr_name",key);
            });
        }

        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                wrapper
        );
        PageUtils pageUtils = new PageUtils(page);
        List<AttrEntity> records = page.getRecords();
        List<AttrRespVo> respVoList = records.stream().map((attrEntity) -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);
            //1设置分类和分组的名字
            if("base".equalsIgnoreCase(type)){
                AttrAttrgroupRelationEntity attr_id = attrAttrgroupRelationDao.selectOne(
                        new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));
                if (attr_id != null && attr_id.getAttrGroupId()!=null) {
                    AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attr_id.getAttrGroupId());
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }


            CategoryEntity category = categoryDao.selectById(attrEntity.getCatelogId());
            if (category != null) {
                attrRespVo.setCatelogName(category.getName());
            }


            return attrRespVo;
        }).collect(Collectors.toList());

        pageUtils.setList(respVoList);
        return pageUtils;
    }

    @Override
    public AttrRespVo getAttrInfo(Long attrId) {
        AttrRespVo attrRespVo=new AttrRespVo();
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(attrEntity,attrRespVo);

        if(attrEntity.getAttrType()==ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode()){
            //1、设置分组信息
            AttrAttrgroupRelationEntity relationEntity = attrAttrgroupRelationDao
                    .selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id",attrId));
            if(relationEntity!=null){
                attrRespVo.setAttrGroupId(relationEntity.getAttrGroupId());
                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(relationEntity.getAttrGroupId());
                if(attrGroupEntity!=null){
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
        }

        //2\设置分类信息
        Long catelogId = attrEntity.getCatelogId();

        Long[] catelogPath = categoryService.findCatelogPath(catelogId);

        attrRespVo.setCatelogPath(catelogPath);
        CategoryEntity category = categoryDao.selectById(catelogId);
        if(category!=null){
            attrRespVo.setCatelogName(category.getName());
        }

        return attrRespVo;
    }

    @Transactional
    @Override
    public void updateAttr(AttrVo attr) {
        AttrEntity attrEntity=new AttrEntity();
        BeanUtils.copyProperties(attr,attrEntity);
        this.updateById(attrEntity);
        if(attrEntity.getAttrType()==ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode()){
            //1、修改分组关联
            AttrAttrgroupRelationEntity relationEntity=new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            relationEntity.setAttrId(attr.getAttrId());
            Integer integer = attrAttrgroupRelationDao
                    .selectCount(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
            if(integer>0){

                attrAttrgroupRelationDao.update(relationEntity,
                        new UpdateWrapper<AttrAttrgroupRelationEntity>().eq("attr_id",attr.getAttrId()));
            }else {
                attrAttrgroupRelationDao.insert(relationEntity);
            }
        }



    }


    /**
     * 根据分组id查找关联的所有基本属性
     * @param attrgroupId
     * @return
     */
    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {

        List<AttrAttrgroupRelationEntity> relationEntities = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_Id", attrgroupId));
        List<Long> attrIds = relationEntities.stream().map((attr) -> {

            return attr.getAttrId();
        }).collect(Collectors.toList());
        if(attrIds==null || attrIds.size()==0){
            return null;
        }

        List<AttrEntity> entities = this.listByIds(attrIds);
        return entities;
    }

    @Override
    public void deleteRelation(AttrGroupRelationVo[] vos) {

        //attrAttrgroupRelationDao.delete(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id",1L).eq("attr_group_id",1L));
        List<AttrAttrgroupRelationEntity> collect = Arrays.asList(vos).stream().map((item) -> {
            AttrAttrgroupRelationEntity entity = new AttrAttrgroupRelationEntity();
            //字段名与类型要对应上
            BeanUtils.copyProperties(item,entity);
            return entity;
        }).collect(Collectors.toList());
        attrAttrgroupRelationDao.deleteBatchRelation(collect);
    }

    /**
     * 获取当前分组没有关联的所有属性
     * @param params
     * @param attrgroupId
     * @return
     */
    @Override
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId) {
        //1\当前分组只能关联自己所属的分类里面的所有属性
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        Long catelogId = attrGroupEntity.getCatelogId();
        //2\当前分组只能关联别的分组没有引用的属性
        //2.1 当前分类下的其他分组
        List<AttrGroupEntity> group = attrGroupDao.selectList(new QueryWrapper<AttrGroupEntity>()
                .eq("catelog_id", catelogId));
        List<Long> collect = group.stream().map(item -> {
            return item.getAttrGroupId();
        }).collect(Collectors.toList());


        //2.2 这些分组关联的属性
        List<AttrAttrgroupRelationEntity> attrGroupId = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>()
                .in("attr_group_id", collect));
        List<Long> attrids = attrGroupId.stream().map(item -> {
            return item.getAttrId();
        }).collect(Collectors.toList());
        //2.3 从当前分类的所有属性中移除这些属性
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>()
                .eq("catelog_id", catelogId)
                .eq("attr_type",ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode())
                ;
        if(attrids!=null && attrids.size()>0){
            wrapper.notIn("attr_id", attrids);
        }
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.and((w)->{
                w.eq("attr_id",key).or().like("attr_name",key);
            });
        }

        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), wrapper);
        return new  PageUtils(page);
    }





}