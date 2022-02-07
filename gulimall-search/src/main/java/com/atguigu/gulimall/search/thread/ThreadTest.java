package com.atguigu.gulimall.search.thread;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadTest {



    public static ExecutorService service=Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
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
         *
         */

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
