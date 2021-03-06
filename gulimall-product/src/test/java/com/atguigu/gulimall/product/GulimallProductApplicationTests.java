package com.atguigu.gulimall.product;

import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.dao.SkuSaleAttrValueDao;
import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.service.BrandService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.SkuItemSaleAttrVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallProductApplicationTests {

    @Autowired
    public BrandService brandService;

    @Autowired
    public CategoryService categoryService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedissonClient redissonClient;


    @Autowired
    AttrGroupDao attrGroupDao;


    @Autowired
    SkuSaleAttrValueDao skuSaleAttrValueDao;


    @Test
    public void test1(){
        //List<SpuItemAttrGroupVo> group = attrGroupDao.getAttrGroupWithAttrBySpuId(13L, 225L);
        //System.out.println(group.toString());
        List<SkuItemSaleAttrVo> saleAttrsBySpuId = skuSaleAttrValueDao.getSaleAttrsBySpuId(13L);
        System.out.println(saleAttrsBySpuId.toString());
    }




    @Test
    public void test(){

        Long[] catelogPath = categoryService.findCatelogPath(218L);
        log.info("完整路径:{}", Arrays.asList(catelogPath));


    }

    @Test
    public void redisson(){

        //System.out.println(redissonClient);

        categoryService.likeTest();

    }



    @Test
    public void testRedis(){

        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        //保存
        ops.set("hello","world"+ UUID.randomUUID().toString());
        //查询
        String hello = ops.get("hello");
        System.out.println("之前保存的数据是："+hello);


    }




    @Test
    public void contextLoads() {

//        BrandEntity brandEntity = new BrandEntity();
//        brandEntity.setBrandId(1l);
//        brandEntity.setName("小米");
//        brandEntity.setDescript("test");
//        brandService.save(brandEntity);
//        brandService.updateById(brandEntity);
//        System.out.println("修改成功。。。。。");
        List<BrandEntity> brand_id = brandService.list(
                new QueryWrapper<BrandEntity>().eq("brand_id", 1));

        brand_id.forEach(x->{
            System.out.println(x);
        });
        System.out.println(brand_id);
    }



}
