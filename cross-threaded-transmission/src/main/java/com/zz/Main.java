package com.zz;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {

    // 用 ThreadLocal<String> 模拟 APM 的 trace 上下文（真实场景是 TraceContext 对象，这里用 traceId 代表）
    static final ThreadLocal<String> TRACE = new ThreadLocal<>();

    public static void main(String[] args) throws Exception {
        // 单线程池保证任务按序执行，方便观察"备份/还原"的效果
        ExecutorService pool = Executors.newSingleThreadExecutor();

        // 1) 直接提交：上下文丢失，工作线程读到 null
        TRACE.set("traceId-1");
        pool.submit(() -> System.out.println("[直接提交] 工作线程读到: " + TRACE.get()));

        // 2) TraceRunner 包装：上下文透传
        TRACE.set("traceId-2");
        pool.submit(TraceRunner.wrap(
                () -> System.out.println("[TraceRunner] 工作线程读到: " + TRACE.get()),
                TRACE
        ));

        // 3) TraceCallable 包装：上下文透传 + 能拿到返回值
        TRACE.set("traceId-3");
        Future<String> future = pool.submit(TraceCallable.wrap(
                () -> TRACE.get(),   // 在工作线程里读取透传过来的值
                TRACE
        ));
        System.out.println("[TraceCallable] 工作线程读到: " + future.get());

        // 4) 任务执行完已还原：下一个未包装的任务拿不到脏数据
        TRACE.set("traceId-4");
        pool.submit(TraceRunner.wrap(
                () -> System.out.println("[TraceRunner] 复用线程读到: " + TRACE.get()),
                TRACE
        ));
        pool.submit(() -> System.out.println("[未包装] 复用线程读到: " + TRACE.get()));

        pool.shutdown();
    }
}
