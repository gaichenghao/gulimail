package com.atguigu.gulimall.product;

import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.service.BrandService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class GulimallProductApplicationTests {

    @Autowired
    BrandService brandService;


    @Test
    void contextLoads() {

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
