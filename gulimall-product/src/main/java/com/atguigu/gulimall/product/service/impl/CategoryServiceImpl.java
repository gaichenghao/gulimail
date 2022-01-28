package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catalog2Vo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {


    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Map<String, Object> cache=new HashMap<>();


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    //查出所有分类以及子分类，以树形结构组装起来
    @Override
    public List<CategoryEntity> listWithTree() {
        //1查出所有分类

        List<CategoryEntity> all = baseMapper.selectList(null);

        //2组装父子树形结构
        //2.1 找到所有一级分类
//        List<CategoryEntity> level1Menus = all.stream().filter((categoryEntity) -> {
//            return categoryEntity.getParentCid() == 0;
//        }).collect(Collectors.toList());

        List<CategoryEntity> level1Menus = all.stream().filter(categoryEntity ->
            categoryEntity.getParentCid() == 0
        ).map(menu->{
            menu.setChildren(getChirdrens(menu,all));
            return menu;
        }).sorted((menu1,menu2)->{
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());

        return level1Menus;
    }

    //批量删除菜单节点
    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 1\检查当前删除的菜单，是否被别的地方引用

        //逻辑删除
        baseMapper.deleteBatchIds(asList);
    }

    /**
     * 找到cateLogid的完整路径
     * 【父、子、孙】
     * @param catelogId
     * @return
     */
    //[2,25,225]
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths=new ArrayList<>();
        List<Long> parentPath=findParentPath(catelogId,paths);
        //数组逆序
        Collections.reverse(parentPath);
        return (Long[]) parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联跟新所有关联数据
     * @param category
     */
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());
    }

    @Override
    public List<CategoryEntity> getLevel1Category() {
        long l = System.currentTimeMillis();
        List<CategoryEntity> parent_cid = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        System.out.println("消耗时间:"+(System.currentTimeMillis()-l));

        return parent_cid;
    }


    @Override
    public Map<Long,List<Catalog2Vo>> getCatalogJson() {
        // 给缓存中放json字符串 拿出的json字符串 还用逆转位能用的对象类型【序列话与方序列化】

        //1\加入缓存逻辑 缓存中存的数据是json
        //json跨语言 跨平台兼容
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if(StringUtils.isEmpty(catalogJSON)){
            //2\缓存中没有 查询数据库
            Map<Long, List<Catalog2Vo>> catalogJsonFromDb = getCatalogJsonFromDb();
            //3\查到数据再放入缓存 将对象转为json放在缓存中
            String s = JSON.toJSONString(catalogJsonFromDb);
            redisTemplate.opsForValue().set("catalogJSON",s);
            return catalogJsonFromDb;
        }
        //转为指定的对象
        Map<Long, List<Catalog2Vo>> longListMap =
                JSON.parseObject(catalogJSON, new TypeReference<Map<Long, List<Catalog2Vo>>>() {
        });
        return longListMap;
    }

    //从数据库查询并粉状分类数据
    public Map<Long,List<Catalog2Vo>> getCatalogJsonFromDb() {

        //1如果缓存中有就用缓存的
        //Map<Long,List<Catalog2Vo>> catalogJson =( Map<Long,List<Catalog2Vo>>) cache.get("catalogJson");
        //if(cache.get("catalogJson")==null){
        // //调用业务 xxxxx
        // //返回数据有放入缓存
        //         //cache.put("catalogJson",parent_cid);
        //
        //}
        //return  catalogJson;

        /**
         * 1、将数据库的多次查询变为一次
         */
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        //1、查出所有1级分类
        List<CategoryEntity> level1Category =  getParent_cid(selectList,0L);
        //2、封装数据
        Map<Long, List<Catalog2Vo>> parent_cid = level1Category.stream().collect(Collectors.toMap(k -> k.getCatId(), v -> {
            //1\每一个的一级分类 查到这个一级分类的二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList,v.getCatId());
            //2 封装上面面的结果
            List<Catalog2Vo> catelog2VoList = new ArrayList<>();
            if (categoryEntities != null) {
                catelog2VoList = categoryEntities.stream().map(l2 -> {
                    Catalog2Vo catelog2Vo = new Catalog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());

                    //1\找当前二级分类的三级分类封装成vo
                    //List<CategoryEntity> level3Catelog= baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", l2.getCatId()));
                    List<CategoryEntity> level3Catelog= getParent_cid(selectList,l2.getCatId());
                    if(level3Catelog!=null){

                        List<Catalog2Vo.Catalog3Vo> collect = level3Catelog.stream().map(l3 -> {
                            //2封装指定格式
                            Catalog2Vo.Catalog3Vo catelog3Vo = new Catalog2Vo.Catalog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(collect);
                    }



                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2VoList;
        }));

        return parent_cid;




    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList,long parent_cid) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> item.getParentCid() == parent_cid).collect(Collectors.toList());
        return  collect;
        //return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
    }

    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if(byId.getParentCid()!=0){
            findParentPath(byId.getParentCid(),paths);
        }
        return paths;
    }

    //递归查找菜单的子菜单
    private List<CategoryEntity> getChirdrens(CategoryEntity root,List<CategoryEntity> all){
        List<CategoryEntity> categoryEntityList = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            //1找到子菜单
            categoryEntity.setChildren(getChirdrens(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            //2菜单的排序
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());
        return categoryEntityList;
    }

}