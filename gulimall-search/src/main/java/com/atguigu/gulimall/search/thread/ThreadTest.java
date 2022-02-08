package com.atguigu.gulimall.search.thread;

import java.util.concurrent.*;

public class ThreadTest {


    public static ExecutorService service=Executors.newFixedThreadPool(10);


    public static void main(String[] args) throws ExecutionException, InterruptedException {

        System.out.println("main----start");
        //CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        //    System.out.println("当前线程：" + Thread.currentThread().getId());
        //    int i = 10 / 2;
        //    System.out.println("运行结果" + i + "-------------" + Thread.currentThread().getId());
        //}, service);
        /**
         * 方法执行完成后的感知
         */
        //CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("当前线程：" + Thread.currentThread().getId());
        //    int i = 10 / 0;
        //    System.out.println("运行结果" + i + "-------------" + Thread.currentThread().getId());
        //    return i;
        //}, service).whenComplete((res,excption)->{
        //    //虽然能得到异常信息 但是没法修改返回数据
        //    System.out.println("异步任务完成了 。。。。结果是："+res+";异常时："+excption);
        //}).exceptionally(throwa->{
        //    //可以感知异常 同时返回默认值
        //    return 10;
        //});
        /**
         * 方法执行完成后的处理
         */
        //CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("当前线程：" + Thread.currentThread().getId());
        //    int i = 10 / 4;
        //    System.out.println("运行结果" + i + "-------------" + Thread.currentThread().getId());
        //    return i;
        //}, service).handle((res,thr)->{
        //    if(res!=null){
        //        return res*2;
        //    }
        //    if(thr!=null){
        //        return 0;
        //    }
        //    return 0;
        //});
        //R apply（T t,U u）;

        /**
         * 线程串行化
         * 1） thenRun：不能获取到上一步的执行结果 无返回值
         * .thenRunAsync(() -> {
         *             System.out.println("任务2启动了。。。");
         *         }, service);
         *
         *2)\能接受上一步结果 但是无返回值 future无返回值
         * .thenAcceptAsync(res->{
         *             System.out.println("任务2启动了。。。");
         *         },service);
         * 3) thenApplyAsync 能接受上一步结果 有返回值
         */
        //CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("当前线程：" + Thread.currentThread().getId());
        //    int i = 10 / 4;
        //    System.out.println("运行结果" + i + "-------------" + Thread.currentThread().getId());
        //    return i;
        //}, service).thenApplyAsync(res -> {
        //    System.out.println("任务2启动了。。。");
        //    return "HELLO" + res;
        //}, service);
        //
        //String s = future.get();
        /**
         * 两个都完成
         */
        //CompletableFuture<Object> future01 = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("任务1线程：" + Thread.currentThread().getId());
        //    int i = 10 / 4;
        //    System.out.println("任务1运行结果" + i + "-------------" + Thread.currentThread().getId());
        //    return i;
        //}, service);
        //
        //CompletableFuture<Object> future02 = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("任务2线程：" + Thread.currentThread().getId());
        //
        //
        //
        //    try {
        //        Thread.sleep(3000);
        //        System.out.println("任务2运行结果" + "-------------" );
        //    } catch (InterruptedException e) {
        //        e.printStackTrace();
        //    }
        //    return "hello";
        //}, service);


        //future01.runAfterBothAsync(future02,()->{
        //    System.out.println("任务3开始。。。");
        //},service);

        //void accept(T t,U u);
        //future01.thenAcceptBothAsync(future02,(f1,f2)->{
        //    System.out.println("任务3开始。。。之前的结果："+f1+"-->>"+f2);
        //},service);

        //CompletableFuture<String> future = future01.thenCombineAsync(future02, (f1, f2) -> {
        //    return f1 + ":" + f2 + "-> haha";
        //}, service);

        /**
         * 两个任务 只要有一个完成 我们就执行任务3
         * runAfterEitherAsync:不感知结果 自己也无返回值
         * acceptEitherAsync:感知结果 自己也无返回值
         * applyToEitherAsync"：感知结果 自己有返回值
         */
        //future01.runAfterEitherAsync(future02,()->{
        //    System.out.println("任务3开始。。。之前的结果：");
        //},service);
        //future01.acceptEitherAsync(future02,(res)->{
        //    System.out.println("任务3开始。。。之前的结果："+res);
        //},service);

        //CompletableFuture<String> future = future01.applyToEitherAsync(future02, res -> {
        //    System.out.println("任务3开始。。。之前的结果：");
        //    return res.toString() + "->哈哈";
        //}, service);
        //String s = future.get();
        //System.out.println(s);

        CompletableFuture<String> futureImg = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的图片信息");
            return "hello.jpg";
        },service);

        CompletableFuture<String> futureAttr = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的属性");
            return "黑色+256g";
        },service);


        CompletableFuture<String> futureDesc = CompletableFuture.supplyAsync(() -> {

            try {

                Thread.sleep(3000);
                System.out.println("查询商品的介绍");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "华为";
        },service);
        //futureImg.get();futureAttr.get();futureDesc.get();
        //CompletableFuture<Void> allOf = CompletableFuture.allOf(futureImg, futureAttr, futureDesc);
        CompletableFuture<Object> anyOf = CompletableFuture.anyOf(futureImg, futureAttr, futureDesc);
        anyOf.get();//等待所有结果完成


        //System.out.println("main----end--------"+futureImg.get()+"->"+futureAttr.get()+"->"+futureDesc.get());
        System.out.println("main----end--------"+anyOf.get());
    }

    public static void thread(String[] args) throws ExecutionException, InterruptedException {
        /**
         *  1\继承thread
         *
         *         Thread01 t=new Thread01();
         *         t.start();
         *  2、实现runnable
         *          Runable01 runable01=new Runable01();
         *         new Thread(runable01).start();
         *  3、实现callable接口+futuretask（可以拿到返回结果 可以处理异常）
         *          FutureTask<Integer> futureTask=new FutureTask<>(new Callable01());
         *         new Thread(futureTask).start();
         *
         *         //阻塞等待整个线程执行完成 ，获取放回结果
         *         Integer integer = futureTask.get();
         *  4、线程池
         *      给线程池直接提交任务
         *              service.execute(new Runable01());
         *       1、创建：
         *       1）Executors
         *       2）new ThreadPoolExecutor
         *
         *       Future 可以获取到异步结果
         *
         *
         *      区别：
         *      1、2 不能得到返回值 3可以获取放回值
         *      123 不能控制资源
         *      4可以控制资源 性能稳定
         */
        System.out.println("main----start");
        //我们以后在业务代码里面了 以上三种启动线程的方式都不用，【将所有的多线程异步任务都交给线程池执行】
        //new Thread(()-> System.out.println("hello")).start();

        //当前系统中池只有一两个，每个异步任务 提交给线程池让他自己去执行就行
        /**
         * 七大参数
         * corePoolSize：[5] 核心线程数[一直存在除非（allowcorethreadtimeout）] 线程池 创建好以后就准备就绪的线程数量 就等待来接受异步任务去执行
         *      5个 Thread thread=new Thread（）； thread.start();
         * maximumPoolSize：【200】 最大线程数量大于core数量
         * keepAliveTime：存活时间 如果当前的线程数量大于core数量
         *      释放空闲的线程 只要线程空闲大于指定的keepAliveTime；
         * unit:时间单位
         * BlockingQueue<runnable> workQueue :阻塞队列 如果任务有很多 就会将目前多的任务放在队列里面
         *              只要线程空闲 就会去队列里面取出新的任务继续执行
         *
         * threadFactory：线程的创建工厂
         *
         * RejectedExecutionHandler handler：如果队列慢了 按照我们指定的拒绝策略拒绝执行任务
         *工作顺序：
         * 1)线程池创建 准备好core数量的核心线程 准备接受任务
         * 1、1 core满了 就将在进来的任务放入阻塞队列中 空闲的core就会自己去阻塞队列 获取任务执行
         * 1、2 阻塞队列满了 就直接开心线程执行 最大只能开到max指定的数量
         * 1、3 max满了就用RejectedExecutionHandler拒绝任务
         * 1、4 max都执行完成 有很多空闲 在指定的时间keepAliveTime以后 释放max-core 这些线程
         *
         * new LinkedBlockingDeque<>(),默认是Integer的最大值 内存不够、
         *
         *
         * 一个线程池 core 7 max 20 queue 50 100并发进来怎么分配
         * 7个会立即执行 50个会进入队列 再开13个进行执行 剩下的30个就使用拒绝策略
         *
         * 如果不想抛弃还要执行 CallerRunsPolicy;
         *
         *
         */


        ThreadPoolExecutor executor=new ThreadPoolExecutor(5
                ,200
                , 10
                , TimeUnit.SECONDS
                , new LinkedBlockingDeque<>(100000)
                , Executors.defaultThreadFactory()
                ,new ThreadPoolExecutor.AbortPolicy());

        //Executors.newCachedThreadPool() core是0 所有都可回收
        //Executors.newFixedThreadPool() 固定大小
        //Executors.newScheduledThreadPool() 定时任务的线程池
        //Executors.newSingleThreadExecutor() 单线程的线程池 后台从队列里面获取任务 挨个执行
        service.execute(new Runable01());

        System.out.println("main----end-------");
    }

    public static class Thread01 extends Thread{

        @Override
        public void run() {
            System.out.println("当前线程："+Thread.currentThread().getId());
            int i=10/2;
            System.out.println("运行结果"+i);
        }
    }

    public static class Runable01 implements Runnable{

        @Override
        public void run() {
            System.out.println("当前线程："+Thread.currentThread().getId());
            int i=10/2;
            System.out.println("运行结果"+i);
        }
    }

    public static class Callable01 implements Callable<Integer>{


        @Override
        public Integer call() throws Exception {
            System.out.println("当前线程："+Thread.currentThread().getId());
            int i=10/2;
            System.out.println("运行结果"+i);
            return i;
        }
    }
}
