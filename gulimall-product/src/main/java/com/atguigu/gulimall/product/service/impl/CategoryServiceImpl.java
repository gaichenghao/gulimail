package com.atguigu.gulimall.product.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

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