package com.zz;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 对比 wrap() 静态工厂 vs new 构造器，在泛型推导场景下的差异。
 */
public class WrapApiDemo {

    static final ThreadLocal<String> TRACE = new ThreadLocal<>();

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);

        // A) wrap() 静态工厂：调用点更短，泛型由编译器推导
        TRACE.set("traceId-static");
        Future<String> a = pool.submit(TraceCallable.wrap(TRACE::get, TRACE));
        System.out.println("[wrap 工厂]   读到: " + a.get());

        // B) new 构造器：等价写法
        TRACE.set("traceId-new");
        Future<String> b = pool.submit(new TraceCallable<>(TRACE::get, TRACE));
        System.out.println("[new 构造器] 读到: " + b.get());

        // C) wrap() 让「包装」这个动作在阅读时一目了然
        TRACE.set("traceId-runner");
        pool.submit(TraceRunner.wrap(() ->
                System.out.println("[wrap Runner] 读到: " + TRACE.get()), TRACE));

        pool.shutdown();
        pool.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS);
    }
}