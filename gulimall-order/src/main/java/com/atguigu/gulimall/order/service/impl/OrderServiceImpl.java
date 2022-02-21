package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.order.constant.OrderConstant;
import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.enume.OrderStatusEnum;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.ProductFeignService;
import com.atguigu.gulimall.order.feign.WmsFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.to.OrderCreateTo;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {


    private ThreadLocal<OrderSubmitVo> confirmVoThreadLocal=new ThreadLocal<>();


    @Autowired
    OrderItemService orderItemService;

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    WmsFeignService wmsFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;


    //不能这么写 循环依赖
    //@Autowired
    //OrderService orderService;



    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo=new OrderConfirmVo();
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();

        //主线程拿
        //获取之前的请求
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();



        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            // 1\远程查询所有的收货地址列表
            System.out.println("member线程。。。"+Thread.currentThread().getId());
            //子线程给
            //每一个线程都来共享之前的请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);

            List<MemberAddressVo> address = memberFeignService.getAddress(memberRespVo.getId());
            orderConfirmVo.setAddress(address);
        }, executor);


        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            //2\ 远程查询购物车所有选中的购物项
            System.out.println("cart线程。。。"+Thread.currentThread().getId());
            RequestContextHolder.setRequestAttributes(requestAttributes);

            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();
            orderConfirmVo.setItems(items);
            //feign在远程调用之前要构造请求 调用很多的拦截器
            // RequestInterceptor interceptor :requestInterceptors

        }, executor).thenRunAsync(()->{
            List<OrderItemVo> items = orderConfirmVo.getItems();
            List<Long> collect = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());

            // TODO: 2022/2/17 一定要启动库存服务 ，否则库存查不出
            
            R hasStock = wmsFeignService.getSkusHasStock(collect);
            List<SkuStockVo> data = hasStock.getData(new TypeReference<List<SkuStockVo>>() {
            });
            if(data!=null){
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                orderConfirmVo.setStocks(map);
            }
        },executor);


        //3、查询用户积分
        Integer integration = orderConfirmVo.getIntegration();
        orderConfirmVo.setIntegration(integration);
        //4\其他数据自动计算

        // TODO: 2022/2/17  5|防重令牌
        String toke = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX+memberRespVo.getId(),toke,30, TimeUnit.MINUTES);
        orderConfirmVo.setOrderToken(toke);


        CompletableFuture.allOf(cartFuture,getAddressFuture).get();
        return orderConfirmVo;
    }

    //同一个对象内事务方法互调默认时效 原因 绕过了代理对象
    //事务使用代理对像来控制的
    @Transactional(timeout = 30)
    public void a(){
        //b.c做任何设置都没用 都是和a公用一个事务
        //this.b();//没用
        //this.c();//没用
        OrderServiceImpl  o = (OrderServiceImpl) AopContext.currentProxy();
        o.b();
        o.c();
        int i=10/0;
    }
    @Transactional(propagation = Propagation.REQUIRED,timeout = 2)
    public void b(){
        //7s
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW,timeout = 20)
    public void c(){

    }

    //本地事务 在分布式系统 只能控制住自己的回滚 控制不了其他服务的回滚
    //分布式事务 最大原因 网络问题 +分布式机器
    //(isolation = Isolation.REPEATABLE_READ)
    //REQUIRED/REQUIRED_NEW
    //@GlobalTransactional  //高并发
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        confirmVoThreadLocal.set(vo);
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        SubmitOrderResponseVo response=new SubmitOrderResponseVo();
        response.setCode(0);
        //1\验证令牌[令牌的对比和删除必须保证原子性]
        //0令牌失败 -- 1删除成功
        String script="if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        String orderToken = vo.getOrderToken();
        //原子验证令牌和删除令牌
        Long result = redisTemplate.execute(
                new DefaultRedisScript<Long>(script, Long.class),
                Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId()),
                orderToken);

        if(result==0L){

            response.setCode(1);
            //令牌验证失败
            return response;
        }else {
            //令牌验证成功
            //下单 去创建订单 验令牌 验价格 锁库存
            //1\创建订单 订单项等信息
            OrderCreateTo order = createOrder();
            //2\验价
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();
            if(Math.abs(payAmount.subtract(payPrice).doubleValue())<0.01){
                //金额对比
                //
                //// TODO: 2022/2/21  3\保存订单
                saveOrder(order);
                //4\库存锁定 只要有异常回滚订单数据
                //订单号 所有订单项（skuId，skuName ，num）
                WareSkuLockVo lockVo=new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> locks = order.getOrderItems().stream().map(item -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());
                    orderItemVo.setCount(item.getSkuQuantity());
                    orderItemVo.setTitle(item.getSkuName());
                    return orderItemVo;
                }).collect(Collectors.toList());
                lockVo.setLocks(locks);

                // TODO: 2022/2/18 4.远程锁库存
                //库存成功了 但是网络原因 超时了 订单回滚 库存不滚

                //为了保证高并发 库存服务自己回滚 可以发消息给库存·服务

                //库存服务本身也可以使用自动解锁模式 消息
                R r = wmsFeignService.orderLockStock(lockVo);
                if(r.getCode()==0){
                    //索成功了
                    response.setOrder(order.getOrder());

                    // TODO: 2022/2/21 5、远程扣减积分 出异常
                    //int i=10/0;//订单回滚 库存不滚
                    return  response;
                }else {
                    //锁失败了
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
//                    response.setCode(3);
//                    return  response;

                }


            }else {
                response.setCode(2);
                return response;
            }


        }

        //String redisToken = redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId());
        //if(orderToken!=null && orderToken.equals(redisToken)){
        //    //令牌验证通过
        //    redisTemplate.delete(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId());
        //}else{
        //    //不通过
        //
        //}
    }

    /**
     * 保存订单数据
     * @param order
     */
    private void saveOrder(OrderCreateTo order) {

        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);

        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);

    }

    private OrderCreateTo createOrder(){
        OrderCreateTo orderCreateTo=new OrderCreateTo();
        //1\生成订单号
        String orderSn = IdWorker.getTimeId();
        //创建订单号
        OrderEntity entity = buildOrder(orderSn);
        //2\获取所有订单项
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderSn);

        //3\计算价格相关
        computePrice(entity,orderItemEntities);
        orderCreateTo.setOrder(entity);
        orderCreateTo.setOrderItems(orderItemEntities);

        return  orderCreateTo;
    }

    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {

        BigDecimal total=new BigDecimal("0.0");

        BigDecimal coupon=new BigDecimal("0.0");
        BigDecimal integration=new BigDecimal("0.0");
        BigDecimal promotion=new BigDecimal("0.0");

        BigDecimal gift=new BigDecimal("0.0");
        BigDecimal growth=new BigDecimal("0.0");
        //1\订单价格相关
        //订单的总额 叠加每一个订单想的总额信息
        for (OrderItemEntity entity: orderItemEntities) {
            //BigDecimal multiply = entity.getSkuPrice().multiply(new BigDecimal(entity.getSkuQuantity()));

            BigDecimal realAmount = entity.getRealAmount();
            coupon=coupon.add(entity.getCouponAmount());
            integration=integration.add(entity.getIntegrationAmount());
            promotion=promotion.add(entity.getPromotionAmount());
            total=total.add(realAmount);
            gift=gift.add(new BigDecimal(entity.getGiftIntegration().toString()));
            growth=growth.add(new BigDecimal(entity.getGiftGrowth().toString()));
        }
        orderEntity.setTotalAmount(total);
        //应付总额
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));

        orderEntity.setCouponAmount(coupon);
        orderEntity.setIntegrationAmount(integration);
        orderEntity.setPromotionAmount(promotion);

        //设置订单的相关状态信息
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setAutoConfirmDay(7);


        //设置积分信息
        orderEntity.setIntegration(gift.intValue());
        orderEntity.setGrowth(growth.intValue());
        orderEntity.setDeleteStatus(0);//未删除


    }

    private OrderEntity buildOrder(String orderSn) {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);

        entity.setMemberId(memberRespVo.getId());
        //获取收货地址信息
        OrderSubmitVo orderSubmitVo = confirmVoThreadLocal.get();
        //获取收货地址信息
        R fare = wmsFeignService.getFare(orderSubmitVo.getAddrId());
        FareVo fareResp = fare.getData(new TypeReference<FareVo>() {
        });
        //设置运费信息
        entity.setFreightAmount(fareResp.getFare());
        //设置收货人信息
        entity.setReceiverCity(fareResp.getAddress().getCity());
        entity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
        entity.setReceiverName(fareResp.getAddress().getName());
        entity.setReceiverPhone(fareResp.getAddress().getPhone());
        entity.setReceiverPostCode(fareResp.getAddress().getPostCode());
        entity.setReceiverProvince(fareResp.getAddress().getProvince());
        entity.setReceiverRegion(fareResp.getAddress().getRegion());

        return entity;
    }


    /**
     * 构建所有订单项的数据
     * @param orderSn
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        //最后确定每个购物项的价格
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if(currentUserCartItems!=null && currentUserCartItems.size()>0){
            List<OrderItemEntity> collect = currentUserCartItems.stream().map(cartItem -> {
                OrderItemEntity itemEntity = buildOrderItem(cartItem);

                itemEntity.setOrderSn(orderSn);

                return itemEntity;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;

    }

    /**
     * 构建某一个订单项
     * @param cartItem
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {

        OrderItemEntity itemEntity=new OrderItemEntity();
        //1\订单信息 订单号
        //2\商品的spu信息
        Long skuId = cartItem.getSkuId();
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo data = r.getData(new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(data.getId());
        itemEntity.setSpuBrand(data.getBrandId().toString());
        itemEntity.setSpuName(data.getSpuName());
        itemEntity.setCategoryId(data.getCatalogId());


        //3\商品的sku信息
        itemEntity.setSkuId(cartItem.getSkuId());
        itemEntity.setSkuName(cartItem.getTitle());
        itemEntity.setSkuPic(cartItem.getImage());
        itemEntity.setSkuPrice(cartItem.getPrice());
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        itemEntity.setSkuAttrsVals(skuAttr);
        itemEntity.setSkuQuantity(cartItem.getCount());
        //4\优惠信息【不做】
        //5\积分信息
        itemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        itemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());

        //6\订单项的价格信息
        itemEntity.setPromotionAmount(new BigDecimal("0"));
        itemEntity.setCouponAmount(new BigDecimal("0"));
        itemEntity.setIntegrationAmount(new BigDecimal("0"));
        //当前订单项的实际金额 总额-各种优惠
        BigDecimal multiply = itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString()));
        BigDecimal subtract=multiply
                .subtract(itemEntity.getCouponAmount())
                .subtract(itemEntity.getCouponAmount())
                .subtract(itemEntity.getIntegrationAmount());
        itemEntity.setRealAmount(subtract);

        return itemEntity;
    }

}