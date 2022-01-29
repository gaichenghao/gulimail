package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catalog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redisson;

    @Autowired
    RedisTemplate redisTemplate;

    @GetMapping({"/","/index.html"})
    public String indexPage(Model model){
        
        //// TODO: 2022/1/25 查出所有的1级分类
        List<CategoryEntity> categories=categoryService.getLevel1Category();


        //试图解析器进行频串:
        //classpath:/templates/+返回值+：.html
        model.addAttribute("categorys",categories);
        return "index";
    }

    //index/catalog.json
    @ResponseBody
    @GetMapping("index/catalog.json")
    public Map<Long, List<Catalog2Vo>> getCatalogJson(){
        Map<Long, List<Catalog2Vo>> map=categoryService.getCatalogJson();
        return map;
    }

    @ResponseBody
    @GetMapping("/hello")
    private String hello(){
        //1\获取一把锁 只要锁的名字一样 就是同一把锁
        RLock lock = redisson.getLock("my-lock");

        //2\加锁
        lock.lock();//阻塞式等待 默认加的锁都是30s时间
        //1）、锁的自动续期 如果业务超长 运行期间自动给锁续上新的30s 不用担心业务时间长 锁自动过期被删掉
        //2)\枷锁的业务只要运行完成 如不会给当前锁续期 即使不手动解锁 锁默认在30s以后自动删除

        //lock.lock(10, TimeUnit.SECONDS);//10秒自动解锁 自动解锁时间一定要大于业务的执行时间。
        //问题 lock.lock(10, TimeUnit.SECONDS); 在所时间到了之后 不会自动续期
        //1\如果我们传递了锁的超时时间，就发送给redis执行脚本 进行站锁 默认超时时间就是我们指定的是时间
        //2 如果我们未指定锁的超时时间，就使用30*1000【lockwatchdogtimeout看门狗的默认时间】
        //    只要站锁成功 就会启动一个定时任务【重新给锁设置过期时间新的过期时间就是看门狗的默认时间】 每隔10s都会自动再次续期 续成30s
        // lockwatchdogtimeout[看门狗时间]/3 , 10s

        //最佳实战
        //  1)、lock.lock(30, TimeUnit.SECONDS); 省掉了整个续期操作 手动解锁
        try {
            System.out.println("加锁成功，执行业务。。。"+Thread.currentThread().getId());
            Thread.sleep(30000);
        }catch (Exception e){


        }finally {
            //3\解锁 将设解锁代码没有运行 redisson会不会出现死锁
            System.out.println("释放锁。。。"+Thread.currentThread().getId());
            lock.unlock();
        }
        return "hello";
    }


    //保证一定能读到最新数据 修改期间 写锁是一个排它锁（互斥锁）。读锁是一个共享锁
    //写锁你没释放读就必须等待
    @GetMapping("/write")
    @ResponseBody
    public String writeValue(){
        RReadWriteLock readWriteLock = redisson.getReadWriteLock("rw-lock");
        String s ="";
        RLock rLock = readWriteLock.writeLock();
        try {
            //1 该数据加写锁 读数据加读锁

            rLock.lock();
            s= UUID.randomUUID().toString();
            Thread.sleep(30000);
            redisTemplate.opsForValue().set("writeValue",s);
        }catch (Exception ex){

        }finally {
            rLock.unlock();
        }
        return s;
    }

    @GetMapping("/read")
    @ResponseBody
    public String readValue(){
        RReadWriteLock readWriteLock = redisson.getReadWriteLock("rw-lock");
        String s ="";
        //加读锁
        RLock rLock = readWriteLock.readLock();
        rLock.lock();
        try {

            s = (String) redisTemplate.opsForValue().get("writeValue");
        }catch (Exception ex){

        }finally {
            rLock.unlock();
        }
        return s;
    }




}
