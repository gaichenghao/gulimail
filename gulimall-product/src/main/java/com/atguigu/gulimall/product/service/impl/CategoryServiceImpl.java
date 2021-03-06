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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {


    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    RedissonClient redisson;

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
     *@CacheEvict 失效模式
     * 1\同时进行多种缓存操作 @Caching
     * 2、指定删除某个分区下的所有数据     @CacheEvict(value = "category" ,allEntries = true)
     * 3、存储统一类型的数据 都可以指定同一个分区。分区名默认就是缓存的前缀
     * @param category
     */

//    @Caching(evict = {
//            @CacheEvict(value = "category",key = "'getLevel1Category'"),
//            @CacheEvict(value = "category",key = "'getCatalogJson'")
//    })
    //category:key
    @CacheEvict(value = "category" ,allEntries = true)//失效模式
//    @CachePut //双写模式
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());
        //同时修改缓存中的数据
        //redis.del("catalogJson");//等待下次主动查询进行更新
    }

    /**
     *     1\每一个需要缓存的数据都来指定要放到那个名字的缓存 【缓存的分区（按照业务类型分）】
     *     2\@Cacheable({"category"})
     *     代表当前方法的结果需要缓存 如果缓存中有，方法不用调用
     *     如果缓存中没有 会调用方法 最后将方法的结果放入缓存
     *     3、默认行为
     *      1）如果缓存中有 方法不用调用
     *      2）、key调用自动生成 缓存的名字：：simplekey【】（自动生成的key值）
     *      3）、缓存的value的值 默认使用jdk序列化机制 将序列化后的数据存到redis
     *      4）、默认ttl时间 -1；
     *
     *     自定义：
     *     1）指定生成的缓存使用的key key属性指定 接受一个spel
     *     spel详细参照 https://www.bilibili.com/video/BV1np4y1C7Yf?p=169&spm_id_from=pageDriver  10:36
     *     2）、指定缓存的数据的存活时间:配置文件中修改ttl
     *     3）、将数据保存为json格式：
     *  4、spring-cache 的不足：
     *      1）读模式 ：
     *          缓存穿透：查询一个null数据 解决 缓存空数据 cache-null-values=true
     *          缓存击穿：大量并发进来同时查询一个整好过期的数据 解决:加锁；？默认不加锁 sync = true(加锁 解决击穿)
     *          缓存雪崩：大量的key同时过期 解决 加随机时间 加上过期时间 spring.cache.redis.time-to-live
     *      2）写模式（缓存与数据库一致）
     *          1）、读写加锁。
     *          2）、引入Canal。感知到Mysql的更新去更新数据库
     *          3）、读多写多 直接去数据库查询就行
     *   总结：
     *      常规数据（读多写少 即时性 一致性要求不高的数据） 完全可以使用spring-cache；写模式（只有缓存的数据有过期时间就足够了）
     *      特殊数据：特殊设计
     *
     *
     *   原理：
     *   cachemanager(rediscachemanager)->cache(redisCache)->cache负责缓存的读写
     *
     */


    @Cacheable(value = {"category"},key = "#root.method.name",sync = true)  //代表当前方法的结果需要缓存 如果缓存中有，方法不用调用 如果缓存中没有 会嗲用方法 最后将方法的结果放入缓存
    @Override
    public List<CategoryEntity> getLevel1Category() {
        long l = System.currentTimeMillis();
        List<CategoryEntity> parent_cid = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        System.out.println("消耗时间:"+(System.currentTimeMillis()-l));

        return parent_cid;
//        return null;
    }

    @Cacheable(value = {"category"},key = "#root.methodName")
    @Override
    public Map<Long, List<Catalog2Vo>> getCatalogJson() {
        System.out.println("查询了数据库");

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

    @Override
    public void likeTest() {
        QueryWrapper<CategoryEntity> queryWrapper=new QueryWrapper<>();
        queryWrapper.like("name","图书");
        List<CategoryEntity> categoryEntities = this.baseMapper.selectList(queryWrapper);

    }

    //TODO 产生堆外内存溢出：outofdirectmemoryError
    // 1)\springboot2.0以后默认使用letture作为操作redis的客户端 他使用netty进行网络通信
    // 2) lettuce的bug导致netty堆外内存溢出 -Xmx300m:netty如果没有指定堆外内存 默认使用-xmx300m
    // 可以通过 -Dio.netty.maxDirectMemory 进行设置
    // 1)\ 升级lettuce客户端 2） 切换使用jedis
    // redisTemlate:
    // lettuce、jedis 操作redis 的底层客户端 spring 再次封装redisTemplate;
    //Override
    public Map<Long,List<Catalog2Vo>> getCatalogJson2() {
        // 给缓存中放json字符串 拿出的json字符串 还用逆转位能用的对象类型【序列话与方序列化】


        /**
         * 1、空结果缓存 解决缓存穿透
         * 2、设置过期时间（加随机值） 解决缓存雪崩
         * 3、加锁 解决缓存击穿
         *
         *
         */

        //1\加入缓存逻辑 缓存中存的数据是json
        //json跨语言 跨平台兼容
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if(StringUtils.isEmpty(catalogJSON)){
            //2\缓存中没有 查询数据库

            System.out.println("缓存不命中。。。查询数据库。。。。");
            Map<Long, List<Catalog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbWithRedisLock();

            return catalogJsonFromDb;
        }
        //转为指定的对象
        System.out.println("缓存命中。。。直接返回。。。。");
        Map<Long, List<Catalog2Vo>> longListMap =
                JSON.parseObject(catalogJSON, new TypeReference<Map<Long, List<Catalog2Vo>>>() {
        });
        return longListMap;
    }



    public Map<Long,List<Catalog2Vo>> getCatalogJsonFromDbWithRedisLock() {
        //1、锁的名字 锁的粒度 越细越快
        //锁的粒度 具体缓存的是某个数据 11-号商品 product-11-lock
        RLock catalogJsonlock = redisson.getLock("catalogJson-lock");
        catalogJsonlock.lock();

        Map<Long, List<Catalog2Vo>> dataFromDB;
        try {
            dataFromDB = getDataFromDB();

        }finally {
            catalogJsonlock.unlock();
        }

        return dataFromDB;





        //1\占分布式锁 去redis 占坑
        //String uuid = UUID.randomUUID().toString();
        //Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid,300,TimeUnit.SECONDS);
        //if(lock){
        //    System.out.println("获取分布式锁成功。。。。");
        //    //加锁成功 执行业务
        //    //2\设置过期时间 必须和加锁是同步的 原子的
        //
        //    //redisTemplate.expire("lock",30,TimeUnit.SECONDS);
        //
        //    Map<Long, List<Catalog2Vo>> dataFromDB;
        //    try {
        //        dataFromDB = getDataFromDB();
        //
        //    }finally {
        //        String script ="if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        //        //删除锁
        //        Long lcok1 = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList("lock"), uuid);
        //    }
        //    //获取值对比+对比成功删除=原子操作 luo脚本解锁
        //    //String lockValue = redisTemplate.opsForValue().get("lock");
        //    //if(uuid.equals(lockValue)){
        //    //    //删除我自己的锁
        //    //    redisTemplate.delete("lock");
        //    //}
        //    return dataFromDB;
        //}else{
        //    //加锁失败 重试 synchronized()
        //    //休眠100ms重试
        //    System.out.println("获取分布式锁失败。。。。等待重试");
        //    try {
        //        Thread.sleep(200);
        //    } catch (Exception e) {
        //        e.printStackTrace();
        //    }
        //    return getCatalogJsonFromDbWithRedisLock();//自旋的方式
        //}


    }

    private Map<Long, List<Catalog2Vo>> getDataFromDB() {
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if(!StringUtils.isEmpty(catalogJSON)){
            //转为指定的对象
            Map<Long, List<Catalog2Vo>> longListMap =
                    JSON.parseObject(catalogJSON, new TypeReference<Map<Long, List<Catalog2Vo>>>() {
                    });
            return longListMap;
        }
        System.out.println("查询了数据库");

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

        //3\查到数据再放入缓存 将对象转为json放在缓存中
        String s = JSON.toJSONString(parent_cid);
        redisTemplate.opsForValue().set("catalogJSON",s,1, TimeUnit.DAYS);
        return parent_cid;
    }

    //从数据库查询并粉状分类数据本地锁
    public Map<Long,List<Catalog2Vo>> getCatalogJsonFromDbWithLocalLock() {

        //1如果缓存中有就用缓存的
        //Map<Long,List<Catalog2Vo>> catalogJson =( Map<Long,List<Catalog2Vo>>) cache.get("catalogJson");
        //if(cache.get("catalogJson")==null){
        // //调用业务 xxxxx
        // //返回数据有放入缓存
        //         //cache.put("catalogJson",parent_cid);
        //
        //}
        //return  catalogJson;

        //只要是同一把锁，就能锁住需要这个锁的是一样所有线程
        //1\synchronized(this):springboot所有的组件在容器中都是单利的
        //// TODO: 2022/1/29  本地锁： synchronized juc（lock） 在分布式的情况下想要锁住所有 必须使用分布式锁
        synchronized (this){
            //得到锁以后 我们应该再去缓存中确定一次 如果没有才需要继续查询
            String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
            if(!StringUtils.isEmpty(catalogJSON)){
                //转为指定的对象
                Map<Long, List<Catalog2Vo>> longListMap =
                        JSON.parseObject(catalogJSON, new TypeReference<Map<Long, List<Catalog2Vo>>>() {
                        });
                return longListMap;
            }
            System.out.println("查询了数据库");

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

            //3\查到数据再放入缓存 将对象转为json放在缓存中
            String s = JSON.toJSONString(parent_cid);
            redisTemplate.opsForValue().set("catalogJSON",s,1, TimeUnit.DAYS);
            return parent_cid;



        }








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